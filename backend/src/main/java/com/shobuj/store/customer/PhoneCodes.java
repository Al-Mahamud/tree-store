package com.shobuj.store.customer;

import com.shobuj.store.notify.SmsOutbox;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Six-digit codes sent by text to prove someone holds a phone number: when creating an account and
 * when resetting a password. Codes are stored hashed, expire, allow a few tries, and can't be
 * requested too often for one number or from one address.
 */
@Service
public class PhoneCodes {

    public enum Purpose {
        REGISTER, RESET
    }

    public record Sent(String sentTo, long resendAfterSeconds) {
    }

    static final int PER_ADDRESS_PER_HOUR = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final SmsOutbox outbox;
    private final StoreProperties.Codes settings;
    private final Clock clock;
    private final Map<String, Deque<Instant>> byAddress = new ConcurrentHashMap<>();

    PhoneCodes(JdbcClient jdbc, SmsOutbox outbox, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.settings = properties.codes();
        this.clock = clock;
    }

    /**
     * Creates a code. When {@code deliver} is false (a reset for a number with no account) nothing is
     * texted, but the answer and the limits are the same, so the reply doesn't reveal who has an account.
     */
    @Transactional
    public Sent issue(String phone, Purpose purpose, String address, boolean deliver) {
        Instant now = clock.instant();
        checkAddress(address, now);
        Optional<Instant> last = jdbc.sql("""
                        select max(created_at) from phone_codes where phone = :p and purpose = :u""")
                .param("p", phone).param("u", purpose.name())
                .query((rs, n) -> rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant()).optional();
        if (last.isPresent() && last.get().plus(settings.resendAfter()).isAfter(now)) {
            long wait = Duration.between(now, last.get().plus(settings.resendAfter())).toSeconds() + 1;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "code.too_soon",
                    "A code was just sent. You can ask for another in " + wait + " seconds.");
        }
        int lastHour = jdbc.sql("select count(*) from phone_codes where phone = :p and created_at > :since")
                .param("p", phone).param("since", Timestamp.from(now.minus(Duration.ofHours(1)))).query(Integer.class).single();
        if (lastHour >= settings.perPhonePerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "code.too_many",
                    "Too many codes for this number. Please try again in an hour.");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        jdbc.sql("""
                        insert into phone_codes (phone, purpose, code_hash, expires_at)
                        values (:p, :u, :h, :e)""")
                .param("p", phone).param("u", purpose.name()).param("h", hash(phone, code))
                .param("e", Timestamp.from(now.plus(settings.validFor()))).update();
        if (deliver) {
            outbox.queue(phone, "CODE", "Shobuj code: " + code + ". It expires in " + settings.validFor().toMinutes()
                    + " minutes. Never share it; Shobuj staff will never ask for it.", null, true);
        }
        return new Sent(mask(phone), settings.resendAfter().toSeconds());
    }

    /** Uses up the code if it is right; otherwise counts a wrong try. */
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(String phone, Purpose purpose, String code) {
        record Row(long id, String hash, int attempts) {
        }
        Optional<Row> latest = jdbc.sql("""
                        select id, code_hash, attempts from phone_codes
                         where phone = :p and purpose = :u and used_at is null and expires_at > :now
                         order by created_at desc limit 1 for update""")
                .param("p", phone).param("u", purpose.name()).param("now", Timestamp.from(clock.instant()))
                .query((rs, n) -> new Row(rs.getLong(1), rs.getString(2), rs.getInt(3))).optional();
        ApiException wrong = new ApiException(HttpStatus.BAD_REQUEST, "code.invalid", "Please check the highlighted fields.",
                Map.of("code", "That code isn't right or has expired. Ask for a new one."));
        if (latest.isEmpty() || code == null || !code.trim().matches("\\d{6}")) {
            latest.ifPresent(r -> countTry(r.id(), r.attempts()));
            throw wrong;
        }
        Row row = latest.get();
        boolean matches = MessageDigest.isEqual(row.hash().getBytes(StandardCharsets.UTF_8),
                hash(phone, code.trim()).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            countTry(row.id(), row.attempts());
            throw wrong;
        }
        jdbc.sql("update phone_codes set used_at = now() where id = :id").param("id", row.id()).update();
    }

    private void countTry(long id, int attempts) {
        jdbc.sql("update phone_codes set attempts = attempts + 1, used_at = case when :a >= :max then now() else used_at end where id = :id")
                .param("a", attempts + 1).param("max", settings.maxAttempts()).param("id", id).update();
    }

    private void checkAddress(String address, Instant now) {
        Deque<Instant> times = byAddress.computeIfAbsent(address, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofHours(1)))) {
                times.removeFirst();
            }
            if (times.size() >= PER_ADDRESS_PER_HOUR) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "code.too_many",
                        "Too many codes requested. Please try again later.");
            }
            times.addLast(now);
        }
    }

    /** "017•••••678" */
    static String mask(String phone) {
        return phone.substring(0, 3) + "•••••" + phone.substring(8);
    }

    private static String hash(String phone, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
