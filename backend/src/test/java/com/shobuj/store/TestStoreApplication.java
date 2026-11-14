package com.shobuj.store;

import com.shobuj.store.support.DemoData;
import com.shobuj.store.support.EmbeddedPostgresConfig;
import org.springframework.boot.SpringApplication;

/**
 * Runs the shop locally against an embedded PostgreSQL filled with demo products:
 * {@code ./mvnw spring-boot:test-run}. The database is new on every start.
 */
public class TestStoreApplication {

    public static void main(String[] args) {
        SpringApplication.from(StoreApplication::main)
                .with(EmbeddedPostgresConfig.class, DemoData.class)
                .withAdditionalProfiles("local", "fake-gateway")
                .run(args);
    }
}
