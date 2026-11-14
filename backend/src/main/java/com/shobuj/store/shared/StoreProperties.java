package com.shobuj.store.shared;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code store.*} in application.yml. */
@ConfigurationProperties("store")
public record StoreProperties(Path storageRoot, String mediaUrl, String webUrl, Delivery delivery, Sms sms, Codes codes,
                              Payment payment) {

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

    /** One-time codes that prove a phone number. */
    public record Codes(java.time.Duration validFor, int maxAttempts, java.time.Duration resendAfter, int perPhonePerHour) {
    }
}
