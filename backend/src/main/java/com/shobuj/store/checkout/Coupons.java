package com.shobuj.store.checkout;

import com.shobuj.store.shared.ApiException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Looks up coupon codes and works out what they are worth for a given basket. */
@Service
public class Coupons {

    public enum Kind {
        PERCENT, FIXED, FREE_SHIPPING
    }

    record Coupon(long id, String code, String description, Kind kind, int value, Integer maxDiscount,
                  int minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, int usedCount,
                  boolean active) {
    }

    /** A coupon that applies: how much it takes off the items, and whether delivery becomes free. */
    public record Applied(String code, String description, int discount, boolean freeShipping) {
    }

    private final JdbcClient jdbc;

    Coupons(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public static String normalise(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    /** Throws an {@link ApiException} explaining why the code cannot be used. */
    public Applied apply(String code, int subtotal, Instant now) {
        Coupon coupon = find(normalise(code))
                .orElseThrow(() -> invalid("coupon.not_found", "That code isn't valid."));
        if (!coupon.active() || (coupon.startsAt() != null && now.isBefore(coupon.startsAt()))) {
            throw invalid("coupon.inactive", "That code isn't active.");
        }
        if (coupon.endsAt() != null && !now.isBefore(coupon.endsAt())) {
            throw invalid("coupon.expired", "That code has expired.");
        }
        if (coupon.usageLimit() != null && coupon.usedCount() >= coupon.usageLimit()) {
            throw invalid("coupon.used_up", "That code has been fully used.");
        }
        if (subtotal < coupon.minSubtotal()) {
            throw invalid("coupon.min_subtotal",
                    "Spend ৳" + String.format("%,d", coupon.minSubtotal()) + " or more to use this code.");
        }
        int discount = switch (coupon.kind()) {
            case PERCENT -> {
                int amount = subtotal * coupon.value() / 100;
                yield coupon.maxDiscount() == null ? amount : Math.min(amount, coupon.maxDiscount());
            }
            case FIXED -> Math.min(coupon.value(), subtotal);
            case FREE_SHIPPING -> 0;
        };
        return new Applied(coupon.code(), coupon.description(), discount, coupon.kind() == Kind.FREE_SHIPPING);
    }

    /** Counts one use; fails if the last use was taken by someone else a moment ago. */
    void recordUse(String code) {
        int updated = jdbc.sql("""
                        update coupons set used_count = used_count + 1
                         where code = :code and (usage_limit is null or used_count < usage_limit)""")
                .param("code", code).update();
        if (updated == 0) {
            throw invalid("coupon.used_up", "That code has been fully used.");
        }
    }

    private Optional<Coupon> find(String code) {
        return jdbc.sql("""
                        select id, code, description, kind, value, max_discount, min_subtotal, starts_at, ends_at,
                               usage_limit, used_count, active
                          from coupons where code = :code""")
                .param("code", code)
                .query((rs, n) -> new Coupon(rs.getLong(1), rs.getString(2), rs.getString(3),
                        Kind.valueOf(rs.getString(4)), rs.getInt(5), (Integer) rs.getObject(6), rs.getInt(7),
                        instant(rs.getTimestamp(8)), instant(rs.getTimestamp(9)), (Integer) rs.getObject(10),
                        rs.getInt(11), rs.getBoolean(12)))
                .optional();
    }

    private static Instant instant(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static ApiException invalid(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
