package com.shobuj.store.checkout;

import com.shobuj.store.shared.StoreProperties;
import com.shobuj.store.shared.StoreProperties.Zone;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Delivery fees in taka. The shop changes them in the admin area; until it does, the fees in
 * application.yml ({@code store.delivery}) apply.
 */
@Service
public class DeliveryFees {

    public record Fees(int insideDhaka, int outsideDhaka, int freeOver, Instant updatedAt, String updatedBy) {

        public int fee(Zone zone) {
            return zone == Zone.DHAKA ? insideDhaka : outsideDhaka;
        }
    }

    private final JdbcClient jdbc;
    private final StoreProperties.Delivery defaults;

    DeliveryFees(JdbcClient jdbc, StoreProperties properties) {
        this.jdbc = jdbc;
        this.defaults = properties.delivery();
    }

    public Fees current() {
        return jdbc.sql("select inside_dhaka, outside_dhaka, free_over, updated_at, updated_by from delivery_settings")
                .query((rs, n) -> new Fees(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getTimestamp(4).toInstant(),
                        rs.getString(5)))
                .optional()
                .orElseGet(() -> new Fees(defaults.insideDhaka(), defaults.outsideDhaka(), defaults.freeOver(), null, null));
    }

    @Transactional
    public Fees save(int insideDhaka, int outsideDhaka, int freeOver, String actor) {
        jdbc.sql("""
                        insert into delivery_settings (id, inside_dhaka, outside_dhaka, free_over, updated_by)
                        values (1, :in, :out, :free, :by)
                        on conflict (id) do update set inside_dhaka = :in, outside_dhaka = :out, free_over = :free,
                                                       updated_at = now(), updated_by = :by""")
                .param("in", insideDhaka).param("out", outsideDhaka).param("free", freeOver).param("by", actor).update();
        return current();
    }

    /** The fees for the website: product pages, the bag and checkout show them. */
    @RestController
    static class Api {

        record PublicFees(int insideDhaka, int outsideDhaka, int freeOver) {
        }

        private final DeliveryFees fees;

        Api(DeliveryFees fees) {
            this.fees = fees;
        }

        @GetMapping("/api/v1/delivery")
        PublicFees fees() {
            Fees f = fees.current();
            return new PublicFees(f.insideDhaka(), f.outsideDhaka(), f.freeOver());
        }
    }
}
