package com.shobuj.store.catalog;

import com.shobuj.store.catalog.CatalogViews.CategoryNode;
import com.shobuj.store.catalog.CatalogViews.CategoryRef;
import com.shobuj.store.catalog.CatalogViews.CollectionView;
import com.shobuj.store.catalog.CatalogViews.CareFacts;
import com.shobuj.store.catalog.CatalogViews.Option;
import com.shobuj.store.catalog.CatalogViews.PlanterOption;
import com.shobuj.store.catalog.CatalogViews.Home;
import com.shobuj.store.catalog.CatalogViews.Image;
import com.shobuj.store.catalog.CatalogViews.ProductCard;
import com.shobuj.store.catalog.CatalogViews.ProductDetail;
import com.shobuj.store.shared.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Categories, collections, product pages and the home page. */
@Service
public class CatalogService {

    private final JdbcClient jdbc;
    private final ProductSearch search;

    CatalogService(JdbcClient jdbc, ProductSearch search) {
        this.jdbc = jdbc;
        this.search = search;
    }

    private record CategoryRow(long id, Long parentId, String name, String slug, String description, String image,
                               int ownCount) {
    }

    /** The whole category tree; counts include products in sub-categories. */
    public List<CategoryNode> categoryTree() {
        List<CategoryRow> rows = jdbc.sql("""
                        select c.id, c.parent_id, c.name, c.slug, c.description, c.image_url,
                               (select count(*) from products p where p.category_id = c.id and p.status = 'ACTIVE')
                          from categories c
                         order by c.position, c.name
                        """)
                .query((rs, n) -> new CategoryRow(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7)))
                .list();
        Map<Long, List<CategoryRow>> byParent = new HashMap<>();
        for (CategoryRow row : rows) {
            byParent.computeIfAbsent(row.parentId() == null ? 0L : row.parentId(), k -> new ArrayList<>()).add(row);
        }
        return build(0L, byParent);
    }

    private List<CategoryNode> build(long parentId, Map<Long, List<CategoryRow>> byParent) {
        List<CategoryNode> nodes = new ArrayList<>();
        for (CategoryRow row : byParent.getOrDefault(parentId, List.of())) {
            List<CategoryNode> children = build(row.id(), byParent);
            int count = row.ownCount() + children.stream().mapToInt(CategoryNode::productCount).sum();
            nodes.add(new CategoryNode(row.id(), row.name(), row.slug(), row.description(), row.image(), count,
                    children));
        }
        return nodes;
    }

    public CategoryNode category(String slug) {
        return find(categoryTree(), slug).orElseThrow(() -> ApiException.notFound("category"));
    }

    private static java.util.Optional<CategoryNode> find(List<CategoryNode> nodes, String slug) {
        for (CategoryNode node : nodes) {
            if (node.slug().equals(slug)) {
                return java.util.Optional.of(node);
            }
            var inChild = find(node.children(), slug);
            if (inChild.isPresent()) {
                return inChild;
            }
        }
        return java.util.Optional.empty();
    }

    public List<CollectionView> collections() {
        return jdbc.sql(COLLECTIONS + " where col.active order by col.position, col.name")
                .query(CatalogService::collection).list();
    }

    public CollectionView collection(String slug) {
        return jdbc.sql(COLLECTIONS + " where col.active and col.slug = :slug").param("slug", slug)
                .query(CatalogService::collection).optional()
                .orElseThrow(() -> ApiException.notFound("collection"));
    }

    private static final String COLLECTIONS = """
            select col.id, col.name, col.slug, col.tagline, col.description, col.image_url,
                   (select count(*) from product_collections pc join products p on p.id = pc.product_id
                     where pc.collection_id = col.id and p.status = 'ACTIVE') as n
              from collections col""";

    private static CollectionView collection(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new CollectionView(rs.getLong("id"), rs.getString("name"), rs.getString("slug"),
                rs.getString("tagline"), rs.getString("description"), rs.getString("image_url"), rs.getInt("n"));
    }

    private record ProductRow(long id, String slug, String name, String localName, String botanicalName,
                              long categoryId, String description, String details, CareFacts care,
                              Instant createdAt) {
    }

    public ProductDetail product(String slug) {
        ProductRow p = jdbc.sql("""
                        select id, slug, name, local_name, botanical_name, category_id, description, details,
                               light, water, care_level, placement, pet_friendly, mature_height, growth_rate,
                               fruiting, created_at
                          from products where slug = :slug and status = 'ACTIVE'
                        """)
                .param("slug", slug)
                .query((rs, n) -> new ProductRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8),
                        new CareFacts(rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12),
                                rs.getBoolean(13), rs.getString(14), rs.getString(15), rs.getString(16)),
                        rs.getTimestamp(17).toInstant()))
                .optional()
                .orElseThrow(() -> ApiException.notFound("product"));

        Map<Long, List<Image>> imagesByPlanter = new HashMap<>();
        List<Image> shared = new ArrayList<>();
        jdbc.sql("select planter_id, url, alt from product_images where product_id = :id order by position, id")
                .param("id", p.id())
                .query(rs -> {
                    Long planterId = (Long) rs.getObject(1);
                    Image image = new Image(rs.getString(2), rs.getString(3));
                    if (planterId == null) {
                        shared.add(image);
                    } else {
                        imagesByPlanter.computeIfAbsent(planterId, k -> new ArrayList<>()).add(image);
                    }
                });
        Map<Long, List<Option>> optionsByPlanter = new HashMap<>();
        jdbc.sql("""
                        select planter_id, id, size, height, sku, price, compare_at_price, stock, dhaka_only
                          from variants where product_id = :id
                         order by case size when 'SMALL' then 0 when 'MEDIUM' then 1 else 2 end""")
                .param("id", p.id())
                .query(rs -> {
                    Care.Size size = Care.Size.valueOf(rs.getString(3));
                    optionsByPlanter.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                            .add(new Option(rs.getLong(2), size.name(), size.label, rs.getString(4), rs.getString(5),
                                    rs.getInt(6), (Integer) rs.getObject(7), rs.getInt(8), rs.getBoolean(9)));
                });
        List<PlanterOption> planters = jdbc.sql(
                        "select id, name, kind, hex from planters where product_id = :id order by position, id")
                .param("id", p.id())
                .query((rs, n) -> {
                    long planterId = rs.getLong(1);
                    List<Image> images = new ArrayList<>(imagesByPlanter.getOrDefault(planterId, List.of()));
                    images.addAll(shared);
                    return new PlanterOption(planterId, rs.getString(2), rs.getString(3), rs.getString(4), images,
                            optionsByPlanter.getOrDefault(planterId, List.of()));
                })
                .list();

        List<String> details = Arrays.stream(p.details().split("\n")).map(String::trim).filter(s -> !s.isEmpty())
                .toList();
        return new ProductDetail(p.id(), p.slug(), p.name(), p.localName(), p.botanicalName(),
                breadcrumb(p.categoryId()), p.description(), details, p.care(),
                p.createdAt().isAfter(Instant.now().minus(ProductSearch.NEW_FOR)), planters);
    }

    private List<CategoryRef> breadcrumb(long categoryId) {
        List<CategoryRef> path = jdbc.sql("""
                        with recursive up as (
                            select id, parent_id, name, slug, 0 as depth from categories where id = :id
                            union all
                            select c.id, c.parent_id, c.name, c.slug, up.depth + 1
                              from categories c join up on c.id = up.parent_id)
                        select name, slug from up order by depth desc
                        """)
                .param("id", categoryId)
                .query((rs, n) -> new CategoryRef(rs.getString(1), rs.getString(2)))
                .list();
        return Collections.unmodifiableList(path);
    }

    /** Same category first, then the rest of the parent category. */
    public List<ProductCard> related(String slug, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("slug", slug);
        return search.cards("""
                        p.slug <> :slug and p.category_id in (
                            select c.id from categories c, products me join categories mc on mc.id = me.category_id
                             where me.slug = :slug
                               and (c.id = me.category_id or c.parent_id = coalesce(mc.parent_id, mc.id)))""",
                params,
                "(p.category_id = (select category_id from products where slug = :slug)) desc, p.sold_count desc, p.id",
                limit);
    }

    public Home home() {
        return new Home(
                collections(),
                categoryTree(),
                search.cards("true", Map.of(), "p.sold_count desc, p.id", 8),
                search.cards("p.fruiting is not null", Map.of(), "p.featured desc, p.sold_count desc, p.id", 8),
                search.cards("true", Map.of(), "p.created_at desc, p.id desc", 8));
    }
}
