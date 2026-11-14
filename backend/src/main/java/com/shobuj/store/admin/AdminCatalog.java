package com.shobuj.store.admin;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plants as the shop edits them: care facts, planters, the size × planter options with their own
 * price and stock, pictures (shared or for one planter) and the collections a plant is in.
 */
@Service
public class AdminCatalog {

    public static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");
    public static final List<String> SIZES = List.of("SMALL", "MEDIUM", "LARGE");
    static final Set<String> KINDS = Set.of("GROW_BAG", "CLAY", "CERAMIC");
    static final Set<String> LIGHT = Set.of("LOW", "MEDIUM", "BRIGHT", "FULL_SUN");
    static final Set<String> WATER = Set.of("LOW", "MEDIUM", "HIGH");
    static final Set<String> CARE = Set.of("EASY", "MODERATE", "EXPERT");
    static final Set<String> PLACEMENT = Set.of("INDOOR", "OUTDOOR", "BOTH");
    static final Set<String> GROWTH = Set.of("SLOW", "MEDIUM", "FAST");
    static final Map<String, String> USUAL_HEIGHT = Map.of("SMALL", "1–2 ft", "MEDIUM", "3–4 ft", "LARGE", "5–6 ft");

    /** A planter; {@code id} is null for a new one. */
    public record PlanterInput(Long id, String name, String kind, String hex) {
    }

    /** One size in one planter; {@code planter} is the planter's place in the planters list. */
    public record VariantInput(Long id, Integer planter, String size, String height, String sku, Integer price,
                               Integer compareAtPrice, Integer stock, Boolean dhakaOnly) {
    }

    public record ProductInput(String name, String localName, String botanicalName, String slug, Long categoryId,
                               String description, String details, String light, String water, String careLevel,
                               String placement, Boolean petFriendly, String matureHeight, String growthRate,
                               String fruiting, Boolean featured, String status, List<Long> collectionIds,
                               List<PlanterInput> planters, List<VariantInput> variants) {
    }

    public record PlanterView(long id, String name, String kind, String hex) {
    }

    public record VariantView(long id, long planterId, String size, String height, String sku, int price,
                              Integer compareAtPrice, int stock, boolean dhakaOnly) {
    }

    public record ImageView(long id, Long planterId, String url, String alt) {
    }

    public record ProductView(long id, String name, String localName, String botanicalName, String slug, long categoryId,
                              String category, String description, String details, String light, String water,
                              String careLevel, String placement, boolean petFriendly, String matureHeight,
                              String growthRate, String fruiting, boolean featured, String status, int soldCount,
                              List<Long> collectionIds, List<PlanterView> planters, List<VariantView> variants,
                              List<ImageView> images) {
    }

    public record ProductRow(long id, String name, String localName, String botanicalName, String slug, String image,
                             String category, String status, boolean featured, int optionCount, int fromPrice, int stock,
                             boolean low, int soldCount) {
    }

    public record ProductPage(List<ProductRow> items, int page, int pageSize, long total, Map<String, Integer> counts) {
    }

    public record StockRow(long variantId, long productId, String product, String image, String planter, String size,
                           String height, String sku, int price, int stock, boolean dhakaOnly, String status) {
    }

    public record CollectionOption(long id, String name) {
    }

    private static final String FIRST_IMAGE = """
            (select pi.url from product_images pi left join planters pl on pl.id = pi.planter_id
              where pi.product_id = p.id order by pl.position nulls last, pi.position, pi.id limit 1)""";

    private static final String STOCK_SELECT = """
            select v.id, p.id, p.name, %s, pl.name, v.size, v.height, v.sku, v.price, v.stock, v.dhaka_only, p.status
              from variants v join products p on p.id = v.product_id join planters pl on pl.id = v.planter_id\s""".formatted(FIRST_IMAGE);

    private final JdbcClient jdbc;
    private final StoreProperties properties;
    private final Pictures pictures;

    AdminCatalog(JdbcClient jdbc, StoreProperties properties, Pictures pictures) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.pictures = pictures;
    }

    public ProductPage products(String q, Long categoryId, String status, boolean lowOnly, int page) {
        List<String> where = new ArrayList<>(List.of("true"));
        Map<String, Object> params = new HashMap<>();
        params.put("low", properties.admin().lowStock());
        if (q != null && !q.isBlank()) {
            where.add("(p.name ilike :q or p.local_name ilike :q or p.botanical_name ilike :q or exists "
                    + "(select 1 from variants s where s.product_id = p.id and s.sku ilike :q))");
            params.put("q", "%" + q.trim() + "%");
        }
        if (categoryId != null) {
            where.add("(p.category_id = :cat or p.category_id in (select id from categories where parent_id = :cat))");
            params.put("cat", categoryId);
        }
        if (status != null && STATUSES.contains(status)) {
            where.add("p.status = :status");
            params.put("status", status);
        }
        if (lowOnly) {
            where.add("p.status = 'ACTIVE' and exists (select 1 from variants s where s.product_id = p.id and s.stock <= :low)");
        }
        String condition = String.join(" and ", where);
        int size = 30;
        long total = jdbc.sql("select count(*) from products p where " + condition).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ProductRow> rows = jdbc.sql("""
                        select p.id, p.name, p.local_name, p.botanical_name, p.slug, %s, c.name, p.status, p.featured,
                               (select count(*) from variants s where s.product_id = p.id),
                               coalesce((select min(price) from variants s where s.product_id = p.id), 0),
                               coalesce((select sum(stock) from variants s where s.product_id = p.id), 0),
                               exists (select 1 from variants s where s.product_id = p.id and s.stock <= :low),
                               p.sold_count
                          from products p join categories c on c.id = p.category_id
                         where %s order by p.status = 'ARCHIVED', p.name limit :limit offset :offset"""
                        .formatted(FIRST_IMAGE, condition))
                .params(params)
                .query((rs, n) -> new ProductRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9), rs.getInt(10),
                        rs.getInt(11), rs.getInt(12), rs.getBoolean(13), rs.getInt(14)))
                .list();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : List.of("ACTIVE", "DRAFT", "ARCHIVED")) {
            counts.put(s, 0);
        }
        jdbc.sql("select status, count(*) from products group by status").query(rs -> {
            counts.put(rs.getString(1), rs.getInt(2));
        });
        return new ProductPage(rows, page, size, total, counts);
    }

    public ProductView product(long id) {
        List<PlanterView> planters = jdbc.sql("select id, name, kind, hex from planters where product_id = :id order by position, id")
                .param("id", id)
                .query((rs, n) -> new PlanterView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .list();
        List<VariantView> variants = jdbc.sql("""
                        select v.id, v.planter_id, v.size, v.height, v.sku, v.price, v.compare_at_price, v.stock, v.dhaka_only
                          from variants v join planters pl on pl.id = v.planter_id
                         where v.product_id = :id
                         order by pl.position, pl.id, array_position(array['SMALL', 'MEDIUM', 'LARGE'], v.size)""")
                .param("id", id)
                .query((rs, n) -> new VariantView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getInt(6), (Integer) rs.getObject(7), rs.getInt(8), rs.getBoolean(9)))
                .list();
        List<ImageView> images = jdbc.sql("select id, planter_id, url, alt from product_images where product_id = :id order by position, id")
                .param("id", id)
                .query((rs, n) -> new ImageView(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3), rs.getString(4)))
                .list();
        List<Long> collections = jdbc.sql("select collection_id from product_collections where product_id = :id order by collection_id")
                .param("id", id).query(Long.class).list();
        return jdbc.sql("""
                        select p.id, p.name, p.local_name, p.botanical_name, p.slug, p.category_id, c.name, p.description,
                               p.details, p.light, p.water, p.care_level, p.placement, p.pet_friendly, p.mature_height,
                               p.growth_rate, p.fruiting, p.featured, p.status, p.sold_count
                          from products p join categories c on c.id = p.category_id where p.id = :id""")
                .param("id", id)
                .query((rs, n) -> new ProductView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                        rs.getString(11), rs.getString(12), rs.getString(13), rs.getBoolean(14), rs.getString(15),
                        rs.getString(16), rs.getString(17), rs.getBoolean(18), rs.getString(19), rs.getInt(20),
                        collections, planters, variants, images))
                .optional()
                .orElseThrow(() -> ApiException.notFound("product"));
    }

    public List<CollectionOption> collectionOptions() {
        return jdbc.sql("select id, name from collections order by position, name")
                .query((rs, n) -> new CollectionOption(rs.getLong(1), rs.getString(2))).list();
    }

    /** Creates ({@code id} null) or updates a plant with all of its planters and options. Ones left out are removed. */
    @Transactional
    public ProductView save(Long id, ProductInput in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = trim(in.name());
        if (name == null) {
            errors.put("name", "Please enter the plant's name.");
        }
        if (in.categoryId() == null || jdbc.sql("select count(*) from categories where id = :id").param("id", in.categoryId())
                .query(Integer.class).single() == 0) {
            errors.put("categoryId", "Please choose a category.");
        }
        String status = in.status() == null ? "DRAFT" : in.status();
        if (!STATUSES.contains(status)) {
            errors.put("status", "Choose draft, on sale or archived.");
        }
        choice(errors, "light", in.light(), LIGHT, "Choose how much light it needs.");
        choice(errors, "water", in.water(), WATER, "Choose how much water it needs.");
        choice(errors, "careLevel", in.careLevel(), CARE, "Choose how easy it is to look after.");
        choice(errors, "placement", in.placement(), PLACEMENT, "Choose indoor, outdoor or both.");
        if (trim(in.growthRate()) != null && !GROWTH.contains(in.growthRate())) {
            errors.put("growthRate", "Choose slow, medium or fast.");
        }
        List<PlanterInput> planters = in.planters() == null ? List.of() : in.planters();
        List<VariantInput> variants = in.variants() == null ? List.of() : in.variants();
        if (planters.isEmpty()) {
            errors.put("planters", "Add at least one planter, e.g. Grow bag.");
        }
        Set<String> planterNames = new HashSet<>();
        for (int i = 0; i < planters.size(); i++) {
            PlanterInput p = planters.get(i);
            String at = "planters." + i + ".";
            if (trim(p.name()) == null) {
                errors.put(at + "name", "Name the planter.");
            } else if (!planterNames.add(p.name().trim().toLowerCase(Locale.ROOT))) {
                errors.put(at + "name", "Two planters have this name.");
            }
            if (p.kind() == null || !KINDS.contains(p.kind())) {
                errors.put(at + "kind", "Choose grow bag, clay or ceramic.");
            }
            if (p.hex() == null || !p.hex().matches("#[0-9A-Fa-f]{6}")) {
                errors.put(at + "hex", "Choose a colour.");
            }
        }
        if (status.equals("ACTIVE") && variants.isEmpty()) {
            errors.put("variants", "A plant on sale needs at least one size.");
        }
        Set<String> pairs = new HashSet<>();
        Set<String> skus = new HashSet<>();
        Set<Integer> usedPlanters = new HashSet<>();
        for (int i = 0; i < variants.size(); i++) {
            VariantInput v = variants.get(i);
            String at = "variants." + i + ".";
            if (v.planter() == null || v.planter() < 0 || v.planter() >= planters.size()) {
                errors.put(at + "planter", "Choose a planter.");
            } else {
                usedPlanters.add(v.planter());
            }
            if (v.size() == null || !SIZES.contains(v.size())) {
                errors.put(at + "size", "Choose small, medium or large.");
            } else if (v.planter() != null && !pairs.add(v.planter() + ":" + v.size())) {
                errors.put(at + "size", "This planter already has this size.");
            }
            if (trim(v.sku()) == null) {
                errors.put(at + "sku", "Enter a SKU.");
            } else if (!skus.add(v.sku().trim().toUpperCase(Locale.ROOT))) {
                errors.put(at + "sku", "Two options have this SKU.");
            }
            if (v.price() == null || v.price() <= 0) {
                errors.put(at + "price", "Enter a price above ৳0.");
            } else if (v.compareAtPrice() != null && v.compareAtPrice() <= v.price()) {
                errors.put(at + "compareAtPrice", "The old price must be higher than the price.");
            }
            if (v.stock() != null && (v.stock() < 0 || v.stock() > 100_000)) {
                errors.put(at + "stock", "From 0 to 100000.");
            }
            if (v.height() != null && v.height().trim().length() > 30) {
                errors.put(at + "height", "Keep it short, e.g. 3–4 ft.");
            }
        }
        if (status.equals("ACTIVE")) {
            for (int i = 0; i < planters.size(); i++) {
                if (!usedPlanters.contains(i) && !errors.containsKey("planters." + i + ".name")) {
                    errors.put("planters." + i + ".name", "Add a size for this planter, or remove it.");
                }
            }
        }
        List<Long> collectionIds = in.collectionIds() == null ? List.of()
                : in.collectionIds().stream().filter(Objects::nonNull).distinct().toList();
        if (!collectionIds.isEmpty() && jdbc.sql("select count(*) from collections where id in (:ids)")
                .param("ids", collectionIds).query(Integer.class).single() != collectionIds.size()) {
            errors.put("collectionIds", "One of the collections doesn't exist.");
        }
        String slug = trim(in.slug()) == null ? slug(name == null ? "" : name) : slug(in.slug());
        if (slug.isEmpty()) {
            errors.put("slug", "Enter a web address name using English letters.");
        } else if (jdbc.sql("select count(*) from products where slug = :s and id <> coalesce(cast(:id as bigint), -1)")
                .param("s", slug).param("id", id).query(Integer.class).single() > 0) {
            errors.put("slug", "Another plant already uses this web address.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        Map<String, Object> f = new HashMap<>();
        f.put("name", name);
        f.put("local", trim(in.localName()));
        f.put("botanical", trim(in.botanicalName()));
        f.put("slug", slug);
        f.put("cat", in.categoryId());
        f.put("description", in.description() == null ? "" : in.description().trim());
        f.put("details", in.details() == null ? "" : in.details().strip());
        f.put("light", in.light());
        f.put("water", in.water());
        f.put("care", in.careLevel());
        f.put("placement", in.placement());
        f.put("pet", Boolean.TRUE.equals(in.petFriendly()));
        f.put("height", trim(in.matureHeight()));
        f.put("growth", trim(in.growthRate()));
        f.put("fruiting", trim(in.fruiting()));
        f.put("featured", Boolean.TRUE.equals(in.featured()));
        f.put("status", status);
        long productId;
        if (id == null) {
            productId = jdbc.sql("""
                            insert into products (name, local_name, botanical_name, slug, category_id, description, details,
                                                  light, water, care_level, placement, pet_friendly, mature_height,
                                                  growth_rate, fruiting, featured, status)
                            values (:name, :local, :botanical, :slug, :cat, :description, :details, :light, :water, :care,
                                    :placement, :pet, :height, :growth, :fruiting, :featured, :status) returning id""")
                    .params(f).query(Long.class).single();
        } else {
            jdbc.sql("select id from products where id = :id for update").param("id", id).query(Long.class).optional()
                    .orElseThrow(() -> ApiException.notFound("product"));
            f.put("id", id);
            jdbc.sql("""
                            update products set name = :name, local_name = :local, botanical_name = :botanical, slug = :slug,
                                   category_id = :cat, description = :description, details = :details, light = :light,
                                   water = :water, care_level = :care, placement = :placement, pet_friendly = :pet,
                                   mature_height = :height, growth_rate = :growth, fruiting = :fruiting,
                                   featured = :featured, status = :status, updated_at = now()
                             where id = :id""")
                    .params(f).update();
            productId = id;
        }
        List<Long> planterIds = savePlanters(productId, planters);
        saveVariants(productId, variants, planterIds);
        jdbc.sql("delete from product_collections where product_id = :p").param("p", productId).update();
        for (Long collection : collectionIds) {
            jdbc.sql("insert into product_collections (product_id, collection_id) values (:p, :c)")
                    .param("p", productId).param("c", collection).update();
        }
        return product(productId);
    }

    /** Saves the planters in order and returns their ids in the same order. */
    private List<Long> savePlanters(long productId, List<PlanterInput> planters) {
        List<Long> existing = jdbc.sql("select id from planters where product_id = :p").param("p", productId)
                .query(Long.class).list();
        Set<Long> keep = new HashSet<>();
        for (PlanterInput p : planters) {
            if (p.id() != null) {
                if (!existing.contains(p.id())) {
                    throw ApiException.badRequest("planter.not_found", "One of the planters doesn't belong to this plant.");
                }
                keep.add(p.id());
            }
        }
        for (Long old : existing) {
            if (!keep.contains(old)) {
                // Its pictures stay with the plant as shared pictures; its options go with it.
                jdbc.sql("update product_images set planter_id = null where planter_id = :id").param("id", old).update();
                jdbc.sql("delete from planters where id = :id").param("id", old).update();
            }
        }
        // Park names first, so renaming "Clay pot" ↔ "Grow bag" can't clash.
        jdbc.sql("update planters set name = '~' || id where product_id = :p").param("p", productId).update();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < planters.size(); i++) {
            PlanterInput p = planters.get(i);
            if (p.id() == null) {
                ids.add(jdbc.sql("""
                                insert into planters (product_id, name, kind, hex, position) values (:p, :n, :k, :h, :pos)
                                returning id""")
                        .param("p", productId).param("n", p.name().trim()).param("k", p.kind())
                        .param("h", p.hex().toUpperCase(Locale.ROOT)).param("pos", i).query(Long.class).single());
            } else {
                jdbc.sql("update planters set name = :n, kind = :k, hex = :h, position = :pos where id = :id")
                        .param("n", p.name().trim()).param("k", p.kind()).param("h", p.hex().toUpperCase(Locale.ROOT))
                        .param("pos", i).param("id", p.id()).update();
                ids.add(p.id());
            }
        }
        return ids;
    }

    private void saveVariants(long productId, List<VariantInput> variants, List<Long> planterIds) {
        List<Long> existing = jdbc.sql("select id from variants where product_id = :p").param("p", productId)
                .query(Long.class).list();
        Set<Long> keep = new HashSet<>();
        for (VariantInput v : variants) {
            if (v.id() != null) {
                if (!existing.contains(v.id())) {
                    throw ApiException.badRequest("variant.not_found", "One of the options doesn't belong to this plant.");
                }
                keep.add(v.id());
            }
        }
        for (Long old : existing) {
            if (!keep.contains(old)) {
                jdbc.sql("delete from variants where id = :id").param("id", old).update();
            }
        }
        jdbc.sql("update variants set sku = '~' || id where product_id = :p").param("p", productId).update();
        try {
            for (VariantInput v : variants) {
                Map<String, Object> f = new HashMap<>();
                f.put("product", productId);
                f.put("planter", planterIds.get(v.planter()));
                f.put("size", v.size());
                f.put("height", trim(v.height()) == null ? USUAL_HEIGHT.get(v.size()) : v.height().trim());
                f.put("sku", v.sku().trim().toUpperCase(Locale.ROOT));
                f.put("price", v.price());
                f.put("compare", v.compareAtPrice());
                f.put("stock", v.stock() == null ? 0 : v.stock());
                f.put("dhaka", Boolean.TRUE.equals(v.dhakaOnly()));
                if (v.id() == null) {
                    jdbc.sql("""
                                    insert into variants (product_id, planter_id, size, height, sku, price, compare_at_price,
                                                          stock, dhaka_only)
                                    values (:product, :planter, :size, :height, :sku, :price, :compare, :stock, :dhaka)""")
                            .params(f).update();
                } else {
                    f.put("id", v.id());
                    jdbc.sql("""
                                    update variants set planter_id = :planter, size = :size, height = :height, sku = :sku,
                                                        price = :price, compare_at_price = :compare, stock = :stock,
                                                        dhaka_only = :dhaka
                                     where id = :id""")
                            .params(f).update();
                }
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "variant.sku_taken", "A SKU is already used by another plant.",
                    Map.of("variants", "A SKU is already used by another plant."));
        }
    }

    /** Adds a picture at the end; {@code planterId} null shows it with every planter. */
    @Transactional
    public ProductView addImage(long productId, Long planterId, String alt, byte[] bytes) {
        product(productId);
        checkPlanter(productId, planterId);
        String url = pictures.store("products", bytes);
        int position = jdbc.sql("select coalesce(max(position), -1) + 1 from product_images where product_id = :p")
                .param("p", productId).query(Integer.class).single();
        jdbc.sql("""
                        insert into product_images (product_id, planter_id, url, alt, position)
                        values (:p, :pl, :u, :a, :pos)""")
                .param("p", productId).param("pl", planterId).param("u", url)
                .param("a", alt == null ? "" : alt.trim()).param("pos", position).update();
        touch(productId);
        return product(productId);
    }

    @Transactional
    public ProductView updateImage(long productId, long imageId, Long planterId, String alt) {
        checkPlanter(productId, planterId);
        if (jdbc.sql("update product_images set planter_id = :pl, alt = :a where id = :id and product_id = :p")
                .param("pl", planterId).param("a", alt == null ? "" : alt.trim()).param("id", imageId)
                .param("p", productId).update() == 0) {
            throw ApiException.notFound("image");
        }
        touch(productId);
        return product(productId);
    }

    /** Moves a picture one place earlier (-1) or later (+1). The first picture is the one on cards. */
    @Transactional
    public ProductView moveImage(long productId, long imageId, int direction) {
        List<Long> ids = new ArrayList<>(jdbc.sql("select id from product_images where product_id = :p order by position, id")
                .param("p", productId).query(Long.class).list());
        int at = ids.indexOf(imageId);
        if (at < 0) {
            throw ApiException.notFound("image");
        }
        int to = at + Integer.signum(direction);
        if (to >= 0 && to < ids.size()) {
            ids.remove(at);
            ids.add(to, imageId);
            for (int i = 0; i < ids.size(); i++) {
                jdbc.sql("update product_images set position = :pos where id = :id").param("pos", i).param("id", ids.get(i)).update();
            }
            touch(productId);
        }
        return product(productId);
    }

    /** Removes the picture from the plant. The file stays on disk, since old orders may still show it. */
    @Transactional
    public ProductView deleteImage(long productId, long imageId) {
        if (jdbc.sql("delete from product_images where id = :id and product_id = :p").param("id", imageId)
                .param("p", productId).update() == 0) {
            throw ApiException.notFound("image");
        }
        touch(productId);
        return product(productId);
    }

    public List<StockRow> stock(String q, boolean lowOnly) {
        Map<String, Object> params = new HashMap<>();
        params.put("low", properties.admin().lowStock());
        String where = "p.status <> 'ARCHIVED'";
        if (lowOnly) {
            where += " and v.stock <= :low";
        }
        if (q != null && !q.isBlank()) {
            where += " and (p.name ilike :q or v.sku ilike :q or p.local_name ilike :q or p.botanical_name ilike :q)";
            params.put("q", "%" + q.trim() + "%");
        }
        return jdbc.sql(STOCK_SELECT + """
                         where %s
                         order by p.name, p.id, pl.position, pl.id, array_position(array['SMALL', 'MEDIUM', 'LARGE'], v.size)
                         limit 600""".formatted(where))
                .params(params)
                .query((rs, n) -> stockRow(rs))
                .list();
    }

    @Transactional
    public StockRow setStock(long variantId, int stock) {
        if (stock < 0 || stock > 100_000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                    Map.of("stock", "Enter a number from 0 to 100000."));
        }
        if (jdbc.sql("update variants set stock = :s where id = :id").param("s", stock).param("id", variantId).update() == 0) {
            throw ApiException.notFound("variant");
        }
        return jdbc.sql(STOCK_SELECT + " where v.id = :id").param("id", variantId).query((rs, n) -> stockRow(rs)).single();
    }

    private void checkPlanter(long productId, Long planterId) {
        if (planterId != null && jdbc.sql("select count(*) from planters where id = :id and product_id = :p")
                .param("id", planterId).param("p", productId).query(Integer.class).single() == 0) {
            throw ApiException.badRequest("planter.not_found", "That planter doesn't belong to this plant.");
        }
    }

    private void touch(long productId) {
        jdbc.sql("update products set updated_at = now() where id = :id").param("id", productId).update();
    }

    private static StockRow stockRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StockRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getInt(9), rs.getInt(10), rs.getBoolean(11),
                rs.getString(12));
    }

    private static void choice(Map<String, String> errors, String field, String value, Set<String> allowed, String message) {
        if (value == null || !allowed.contains(value)) {
            errors.put(field, message);
        }
    }

    static String slug(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
