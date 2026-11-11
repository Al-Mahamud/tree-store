package com.shobuj.store.support;

import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * A real PostgreSQL started from bundled binaries, so tests and local runs need neither Docker nor
 * an installed server. Flyway migrates it like any other database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class EmbeddedPostgresConfig {

    /** One server per JVM, shared by every cached Spring context. */
    private static final class Server {

        static final EmbeddedPostgres INSTANCE = start();

        private static EmbeddedPostgres start() {
            try {
                EmbeddedPostgres postgres = EmbeddedPostgres.start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        postgres.close();
                    } catch (IOException ignored) {
                        // The JVM is exiting.
                    }
                }));
                return postgres;
            } catch (IOException e) {
                throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
            }
        }
    }

    @Bean
    DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(Server.INSTANCE.getJdbcUrl("postgres", "postgres"));
        dataSource.setUsername("postgres");
        return dataSource;
    }
}
