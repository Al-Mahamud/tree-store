package com.shobuj.store.catalog;

import com.shobuj.store.catalog.CatalogViews.Facets;
import com.shobuj.store.catalog.CatalogViews.FacetValue;
import com.shobuj.store.catalog.CatalogViews.PriceRange;
import com.shobuj.store.catalog.CatalogViews.ProductCard;
import com.shobuj.store.catalog.CatalogViews.ProductPage;
import com.shobuj.store.catalog.CatalogViews.Swatch;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Finds active plants for grids, with facet counts for the filter panel. */
@Service
public class ProductSearch {

    static final Duration NEW_FOR = Duration.ofDays(30);

    /** Every query reads plants together with their cheapest option's price. */
    private static final String FROM = """
            products p
            cross join lateral (select min(v.price) as min_price, max(v.price) as max_price
                                  from variants v where v.product_id = p.id) mp""";

    private static final TypeReference<List<Swatch>> SWATCHES = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    ProductSearch(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public ProductPage search(ProductQuery query) {
        Where where = where(query, Facet.NONE);
        long total = jdbc.sql("select count(*) from " + FROM + " where " + where.sql)
                .params(where.params).query(Long.class).single();
        List<ProductCard> items = cards(where.sql, where.params, query.sort().orderBy, query.pageSize(),
                (long) query.page() * query.pageSize());
        return new ProductPage(items, query.page(), query.pageSize(), total, facets(query));
    }

    /** Cards for an arbitrary filter: home page rails, related plants. */
    public List<ProductCard> cards(String whereSql, Map<String, ?> params, String orderBy, int limit) {
        return cards("p.status = 'ACTIVE' and (" + whereSql + ")", params, orderBy, limit, 0);
    }

    private List<ProductCard> cards(String whereSql, Map<String, ?> params, String orderBy, int limit, long offset) {
        Map<String, Object> all = new HashMap<>(params);
        all.put("limit", limit);
        all.put("offset", offset);
        String sql = """
                select p.id, p.slug, p.name, p.local_name, c.name as category, mp.min_price, mp.max_price,
                       (select v.compare_at_price from variants v where v.product_id = p.id
                         order by v.price, v.id limit 1) as compare_at,
                       (select coalesce(json_agg(json_build_object('name', pl.name, 'kind', pl.kind, 'hex', pl.hex)
                                                 order by pl.position), '[]')
                          from planters pl where pl.product_id = p.id) as planters,
                       array(select pi.url from product_images pi
                               left join planters pl on pl.id = pi.planter_id
                              where pi.product_id = p.id
                              order by pl.position nulls last, pi.position, pi.id limit 2) as images,
                       p.light, p.care_level, p.pet_friendly, p.fruiting, p.created_at,
                       exists (select 1 from variants v where v.product_id = p.id and v.stock > 0) as in_stock
                  from %s
                  join categories c on c.id = p.category_id
                 where %s
                 order by %s
                 limit :limit offset :offset
                """.formatted(FROM, whereSql, orderBy);
        Instant newSince = Instant.now().minus(NEW_FOR);
        return jdbc.sql(sql).params(all).query((rs, n) -> card(rs, newSince)).list();
    }

    private ProductCard card(ResultSet rs, Instant newSince) throws SQLException {
        String[] images = strings(rs.getArray("images"));
        Integer compareAt = (Integer) rs.getObject("compare_at");
        return new ProductCard(
                rs.getLong("id"), rs.getString("slug"), rs.getString("name"), rs.getString("local_name"),
                rs.getString("category"), rs.getInt("min_price"), compareAt,
                rs.getInt("max_price") != rs.getInt("min_price"),
                images.length > 0 ? images[0] : null, images.length > 1 ? images[1] : null,
                json.readValue(rs.getString("planters"), SWATCHES),
                rs.getString("light"), rs.getString("care_level"), rs.getBoolean("pet_friendly"),
                rs.getString("fruiting"),
                rs.getTimestamp("created_at").toInstant().isAfter(newSince),
                !rs.getBoolean("in_stock"));
    }

    private static String[] strings(Array array) throws SQLException {
        return array == null ? new String[0] : (String[]) array.getArray();
    }

    // ------------------------------------------------------------------ facets

    private Facets facets(ProductQuery query) {
        List<FacetValue> light = enumFacet(query, Facet.LIGHT, "p.light", Care.Light.values(), l -> l.label,
                query.light());
        List<FacetValue> care = enumFacet(query, Facet.CARE, "p.care_level", Care.Level.values(), l -> l.label,
                query.care());
        List<FacetValue> placement = enumFacet(query, Facet.PLACEMENT, "p.placement",
                new Care.Placement[] {Care.Placement.INDOOR, Care.Placement.OUTDOOR}, p -> p.label,
                query.placement() == null ? List.of() : List.of(query.placement()));

        Where forSizes = where(query, Facet.SIZE);
        Map<String, Integer> sizeCounts = counts("""
                select v.size as value, count(distinct p.id) as n
                  from %s join variants v on v.product_id = p.id and v.stock > 0
                 where %s group by v.size""".formatted(FROM, forSizes.sql), forSizes.params);
        List<FacetValue> sizes = Arrays.stream(Care.Size.values())
                .filter(s -> sizeCounts.containsKey(s.name()) || query.sizes().contains(s))
                .map(s -> new FacetValue(s.name(), s.label, sizeCounts.getOrDefault(s.name(), 0),
                        query.sizes().contains(s)))
                .toList();

        Where forPets = where(query, Facet.PETS);
        int pets = jdbc.sql("select count(*) from " + FROM + " where " + forPets.sql + " and p.pet_friendly")
                .params(forPets.params).query(Integer.class).single();

        Where forPrice = where(query, Facet.PRICE);
        PriceRange price = jdbc.sql("select coalesce(min(mp.min_price), 0), coalesce(max(mp.min_price), 0) from "
                        + FROM + " where " + forPrice.sql)
                .params(forPrice.params)
                .query((rs, n) -> new PriceRange(rs.getInt(1), rs.getInt(2)))
                .single();
        return new Facets(light, care, placement, sizes, pets, price);
    }

    private <E extends Enum<E>> List<FacetValue> enumFacet(ProductQuery query, Facet facet, String column,
                                                           E[] values, Function<E, String> label,
                                                           List<E> selected) {
        Where where = where(query, facet);
        Map<String, Integer> counts = counts("select " + column + " as value, count(*) as n from " + FROM
                + " where " + where.sql + " group by " + column, where.params);
        if (facet == Facet.PLACEMENT) {
            // A plant for both places counts towards indoor and outdoor.
            int both = counts.getOrDefault("BOTH", 0);
            counts.merge("INDOOR", both, Integer::sum);
            counts.merge("OUTDOOR", both, Integer::sum);
        }
        return Arrays.stream(values)
                .filter(v -> counts.containsKey(v.name()) || selected.contains(v))
                .map(v -> new FacetValue(v.name(), label.apply(v), counts.getOrDefault(v.name(), 0),
                        selected.contains(v)))
                .toList();
    }

    private Map<String, Integer> counts(String sql, Map<String, Object> params) {
        return jdbc.sql(sql).params(params).query(rs -> {
            Map<String, Integer> counts = new HashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("value"), rs.getInt("n"));
            }
            return counts;
        });
    }

    // ------------------------------------------------------------------ filters

    /** Which filter to leave out, so a facet shows what choosing another value would give. */
    private enum Facet {
        NONE, LIGHT, CARE, PLACEMENT, SIZE, PETS, PRICE
    }

    private record Where(String sql, Map<String, Object> params) {
    }

    private Where where(ProductQuery query, Facet leaveOut) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        conditions.add("p.status = 'ACTIVE'");
        if (query.category() != null) {
            conditions.add("""
                    p.category_id in (
                        with recursive tree as (
                            select id from categories where slug = :category
                            union all
                            select c.id from categories c join tree on c.parent_id = tree.id)
                        select id from tree)""");
            params.put("category", query.category());
        }
        if (query.collection() != null) {
            conditions.add("""
                    p.id in (select pcl.product_id from product_collections pcl
                               join collections col on col.id = pcl.collection_id
                              where col.slug = :collection)""");
            params.put("collection", query.collection());
        }
        String tsQuery = tsQuery(query.q());
        if (tsQuery != null) {
            // Full-text search for words, plus a plain "contains" match so Bangla names
            // (which the text parser may not split into words) are found too.
            conditions.add("""
                    p.search @@ to_tsquery('simple', :tsq)
                    or p.name ilike :like or p.local_name ilike :like or p.botanical_name ilike :like""");
            params.put("tsq", tsQuery);
            params.put("like", "%" + query.q().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (query.sale()) {
            conditions.add("exists (select 1 from variants sv where sv.product_id = p.id and sv.compare_at_price is not null)");
        }
        if (leaveOut != Facet.LIGHT && !query.light().isEmpty()) {
            conditions.add("p.light in (:light)");
            params.put("light", names(query.light()));
        }
        if (leaveOut != Facet.CARE && !query.care().isEmpty()) {
            conditions.add("p.care_level in (:care)");
            params.put("care", names(query.care()));
        }
        if (leaveOut != Facet.PLACEMENT && query.placement() != null) {
            conditions.add("p.placement in (:placement, 'BOTH')");
            params.put("placement", query.placement().name());
        }
        if (leaveOut != Facet.PETS && query.petFriendly()) {
            conditions.add("p.pet_friendly");
        }
        if (leaveOut != Facet.SIZE && !query.sizes().isEmpty()) {
            conditions.add("exists (select 1 from variants fv where fv.product_id = p.id and fv.stock > 0 and fv.size in (:sizes))");
            params.put("sizes", names(query.sizes()));
        }
        if (leaveOut != Facet.PRICE) {
            if (query.minPrice() != null) {
                conditions.add("mp.min_price >= :minPrice");
                params.put("minPrice", query.minPrice());
            }
            if (query.maxPrice() != null) {
                conditions.add("mp.min_price <= :maxPrice");
                params.put("maxPrice", query.maxPrice());
            }
        }
        return new Where(conditions.stream().map(c -> "(" + c + ")").collect(Collectors.joining(" and ")), params);
    }

    private static List<String> names(List<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }

    /** "mang tre" becomes {@code mang:* & tre:*}, so partly typed words still match. */
    static String tsQuery(String q) {
        if (q == null) {
            return null;
        }
        List<String> terms = Arrays.stream(q.toLowerCase().split("[^\\p{L}\\p{N}\\p{M}]+"))
                .filter(t -> !t.isBlank())
                .limit(8)
                .map(t -> t + ":*")
                .toList();
        return terms.isEmpty() ? null : String.join(" & ", terms);
    }
}
