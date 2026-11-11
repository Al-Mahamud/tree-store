package com.shobuj.store.shared;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code store.*} in application.yml. */
@ConfigurationProperties("store")
public record StoreProperties(Path storageRoot, String mediaUrl, Delivery delivery) {

    /** Delivery fees in taka; orders at or above {@code freeOver} (after discounts) ship free. */
    public record Delivery(int insideDhaka, int outsideDhaka, int freeOver) {

        public int fee(Zone zone) {
            return zone == Zone.DHAKA ? insideDhaka : outsideDhaka;
        }
    }

    public enum Zone {
        DHAKA, OUTSIDE
    }
}
