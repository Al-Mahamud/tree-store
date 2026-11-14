package com.shobuj.store.notify;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Texts to send. Adding one is part of the caller's database transaction, so a text goes out only
 * if the change it describes was saved; a background job sends them and retries failures.
 */
@Service
public class SmsOutbox {

    private static final Logger log = LoggerFactory.getLogger(SmsOutbox.class);
    static final int MAX_ATTEMPTS = 3;

    private record Queued(long id, String phone, String body, boolean sensitive, int attempts) {
    }

    private final JdbcClient jdbc;
    private final SmsGateway gateway;

    SmsOutbox(JdbcClient jdbc, SmsGateway gateway) {
        this.jdbc = jdbc;
        this.gateway = gateway;
    }

    public void queue(String phone, String kind, String body, Long orderId, boolean sensitive) {
        jdbc.sql("""
                        insert into sms_messages (phone, kind, body, sensitive, order_id)
                        values (:p, :k, :b, :s, :o)""")
                .param("p", phone).param("k", kind).param("b", body).param("s", sensitive).param("o", orderId)
                .update();
    }

    /** Sends what is waiting. Returns how many went out. */
    public int sendQueued() {
        // Messages stuck in SENDING (the app stopped mid-send) are tried again.
        jdbc.sql("update sms_messages set status = 'QUEUED' where status = 'SENDING' and created_at < now() - interval '5 minutes'")
                .update();
        List<Queued> batch = jdbc.sql("""
                        update sms_messages set status = 'SENDING', attempts = attempts + 1
                         where id in (select id from sms_messages where status = 'QUEUED' order by created_at
                                       limit 20 for update skip locked)
                        returning id, phone, body, sensitive, attempts""")
                .query((rs, n) -> new Queued(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getInt(5)))
                .list();
        int sent = 0;
        for (Queued m : batch) {
            SmsGateway.Result result;
            try {
                result = gateway.send(m.phone(), m.body(), "shobuj-" + m.id());
            } catch (RuntimeException e) {
                log.warn("Sending text {} failed", m.id(), e);
                result = SmsGateway.Result.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            if (result.sent()) {
                sent++;
                jdbc.sql("""
                                update sms_messages set status = 'SENT', sent_at = now(), provider = :pr, provider_id = :id,
                                       error = null,
                                       body = case when sensitive then regexp_replace(body, '[0-9]{6}', '••••••', 'g') else body end
                                 where id = :m""")
                        .param("pr", gateway.name()).param("id", result.providerId()).param("m", m.id()).update();
            } else {
                String next = m.attempts() >= MAX_ATTEMPTS ? "FAILED" : "QUEUED";
                jdbc.sql("update sms_messages set status = :s, provider = :pr, error = :e where id = :m")
                        .param("s", next).param("pr", gateway.name()).param("e", truncate(result.error())).param("m", m.id())
                        .update();
            }
        }
        return sent;
    }

    private static String truncate(String s) {
        return s == null ? null : s.length() > 300 ? s.substring(0, 300) : s;
    }
}
