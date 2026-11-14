package com.shobuj.store.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A new shop starts with the nursery's usual categories, so the owner can add plants straight away.
 * Runs only while there are no categories at all; after that they are edited in the admin area
 * (pictures included). Turn off with {@code store.starter-data=false}.
 */
@Component
@Order(0)
public class StarterCatalog implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StarterCatalog.class);

    /** parent slug (null for top level), name, slug, description. */
    public static final List<String[]> CATEGORIES = List.of(
            new String[] {null, "Trees", "trees", "Fruit, flowering, shade and timber trees for gardens, rooftops and land."},
            new String[] {"trees", "Fruit trees", "fruit-trees", "Grafted trees that fruit years sooner."},
            new String[] {"trees", "Flowering trees", "flowering-trees", "Colour and scent for every season."},
            new String[] {"trees", "Shade & timber", "shade-timber", "Long-lived trees for land and boundaries."},
            new String[] {"trees", "Palms", "palms", "Palms for avenues, gardens and living rooms."},
            new String[] {null, "Indoor plants", "indoor", "Greenery that copes with flats, offices and air conditioning."},
            new String[] {"indoor", "Easy care", "indoor-easy", "Nearly unkillable plants for busy people."},
            new String[] {"indoor", "Statement plants", "indoor-statement", "Big leaves that fill a corner."},
            new String[] {"indoor", "Trailing & hanging", "indoor-trailing", "For shelves, hooks and moss poles."},
            new String[] {null, "Bonsai", "bonsai", "Hand-shaped miniature trees, years in the making."});

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final boolean enabled;

    StarterCatalog(JdbcClient jdbc, TransactionTemplate tx, @Value("${store.starter-data:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || jdbc.sql("select count(*) from categories").query(Long.class).single() > 0) {
            return;
        }
        tx.executeWithoutResult(status -> {
            Map<String, Long> ids = new HashMap<>();
            Map<String, Integer> positions = new HashMap<>();
            for (String[] c : CATEGORIES) {
                String parentKey = c[0] == null ? "" : c[0];
                int position = positions.merge(parentKey, 1, Integer::sum) - 1;
                long id = jdbc.sql("""
                                insert into categories (parent_id, name, slug, description, position)
                                values (:parent, :name, :slug, :description, :position) returning id""")
                        .param("parent", c[0] == null ? null : ids.get(c[0])).param("name", c[1]).param("slug", c[2])
                        .param("description", c[3]).param("position", position)
                        .query(Long.class).single();
                ids.put(c[2], id);
            }
        });
        log.info("Added the starter categories");
    }
}
