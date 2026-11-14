package com.shobuj.store.shared;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code store.*} in application.yml. */
@ConfigurationProperties("store")
public record StoreProperties(Path storageRoot, String mediaUrl, String webUrl, Delivery delivery, Sms sms, Codes codes,
                              Payment payment, Admin admin) {

    /** Delivery fees in taka; orders at or above {@code freeOver} (after discounts) ship free. */
    public record Delivery(int insideDhaka, int outsideDhaka, int freeOver) {

        public int fee(Zone zone) {
            return zone == Zone.DHAKA ? insideDhaka : outsideDhaka;
        }
    }

    public enum Zone {
        DHAKA, OUTSIDE
    }

    /** Text messages: which sender to use. Provider credentials are read by the sender itself. */
    public record Sms(String driver) {
    }

    /** Online payment: how long an unpaid order holds its plants, and how many payment tries it gets. */
    public record Payment(java.time.Duration expiry, int maxAttempts) {
    }

    /**
     * The admin area: the first staff account, created from ADMIN_PHONE / ADMIN_PASSWORD when the shop
     * has none, and the stock level at or below which an option counts as running low.
     */
    public record Admin(String firstStaffName, String firstStaffPhone, String firstStaffPassword, int lowStock) {
    }

    /** One-time codes that prove a phone number. */
    public record Codes(java.time.Duration validFor, int maxAttempts, java.time.Duration resendAfter, int perPhonePerHour) {
    }
}
