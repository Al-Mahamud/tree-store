package com.shobuj.store.admin;

import com.shobuj.store.shared.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coupons, collections and the text-message log. */
@Service
public class AdminSettings {

    public record CouponInput(String code, String description, String kind, Integer value, Integer maxDiscount,
                              Integer minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, Boolean active) {
    }

    public record CouponView(long id, String code, String description, String kind, int value, Integer maxDiscount,
                             int minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, int usedCount,
                             boolean active) {
    }

    public record CollectionInput(String name, String tagline, String description, Boolean active) {
    }

    public record CollectionView(long id, String name, String slug, String tagline, String description, String image,
                                 boolean active, int position, int products) {
    }

    public record Text(long id, String phone, String kind, String body, String status, int attempts, String provider,
                       String error, String orderNumber, Instant createdAt, Instant sentAt) {
    }

    public record TextPage(List<Text> items, int page, int pageSize, long total, Map<String, Integer> counts) {
    }

    private final JdbcClient jdbc;
    private final Pictures pictures;

    AdminSettings(JdbcClient jdbc, Pictures pictures) {
        this.jdbc = jdbc;
        this.pictures = pictures;
    }

    public List<CouponView> coupons() {
        return jdbc.sql("""
                        select id, code, description, kind, value, max_discount, min_subtotal, starts_at, ends_at,
                               usage_limit, used_count, active
                          from coupons order by active desc, created_at desc""")
                .query((rs, n) -> new CouponView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), (Integer) rs.getObject(6), rs.getInt(7), instant(rs.getTimestamp(8)),
                        instant(rs.getTimestamp(9)), (Integer) rs.getObject(10), rs.getInt(11), rs.getBoolean(12)))
                .list();
    }

    @Transactional
    public CouponView saveCoupon(Long id, CouponInput in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = in.code() == null ? "" : in.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9]{3,20}")) {
            errors.put("code", "3 to 20 letters and numbers, e.g. EID200.");
        }
        String kind = in.kind() == null ? "" : in.kind();
        int value = in.value() == null ? 0 : in.value();
        switch (kind) {
            case "PERCENT" -> {
                if (value < 1 || value > 90) {
                    errors.put("value", "A percentage from 1 to 90.");
                }
            }
            case "FIXED" -> {
                if (value < 1) {
                    errors.put("value", "Enter the amount in taka.");
                }
            }
            case "FREE_SHIPPING" -> value = 0;
            default -> errors.put("kind", "Choose what the coupon gives.");
        }
        if (in.maxDiscount() != null && in.maxDiscount() < 1) {
            errors.put("maxDiscount", "Leave empty or enter an amount above ৳0.");
        }
        if (in.minSubtotal() != null && in.minSubtotal() < 0) {
            errors.put("minSubtotal", "Can't be below ৳0.");
        }
        if (in.usageLimit() != null && in.usageLimit() < 1) {
            errors.put("usageLimit", "Leave empty for no limit, or enter 1 or more.");
        }
        if (in.startsAt() != null && in.endsAt() != null && !in.endsAt().isAfter(in.startsAt())) {
            errors.put("endsAt", "The end must be after the start.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("code", code);
        f.put("description", in.description() == null ? "" : in.description().trim());
        f.put("kind", kind);
        f.put("value", value);
        f.put("max", kind.equals("PERCENT") ? in.maxDiscount() : null);
        f.put("min", in.minSubtotal() == null ? 0 : in.minSubtotal());
        f.put("starts", in.startsAt() == null ? null : Timestamp.from(in.startsAt()));
        f.put("ends", in.endsAt() == null ? null : Timestamp.from(in.endsAt()));
        f.put("limit", in.usageLimit());
        f.put("active", in.active() == null || in.active());
        try {
            if (id == null) {
                id = jdbc.sql("""
                                insert into coupons (code, description, kind, value, max_discount, min_subtotal, starts_at,
                                                     ends_at, usage_limit, active)
                                values (:code, :description, :kind, :value, :max, :min, :starts, :ends, :limit, :active)
                                returning id""")
                        .params(f).query(Long.class).single();
            } else {
                f.put("id", id);
                if (jdbc.sql("""
                                update coupons set code = :code, description = :description, kind = :kind, value = :value,
                                       max_discount = :max, min_subtotal = :min, starts_at = :starts, ends_at = :ends,
                                       usage_limit = :limit, active = :active
                                 where id = :id""").params(f).update() == 0) {
                    throw ApiException.notFound("coupon");
                }
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "coupon.code_taken", "Please check the highlighted fields.",
                    Map.of("code", "Another coupon already uses this code."));
        }
        long found = id;
        return coupons().stream().filter(c -> c.id() == found).findFirst().orElseThrow();
    }

    public List<CollectionView> collections() {
        return jdbc.sql("""
                        select c.id, c.name, c.slug, c.tagline, c.description, c.image_url, c.active, c.position,
                               (select count(*) from product_collections pc join products p on p.id = pc.product_id
                                 where pc.collection_id = c.id and p.status <> 'ARCHIVED')
                          from collections c order by c.position, c.id""")
                .query((rs, n) -> new CollectionView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getBoolean(7), rs.getInt(8), rs.getInt(9)))
                .list();
    }

    /**
     * Creates ({@code id} null) or edits a collection. Its web address is fixed when it is made, so
     * links keep working after a rename. Plants are added to collections in the plant editor.
     */
    @Transactional
    public List<CollectionView> saveCollection(Long id, CollectionInput in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = in.name() == null ? "" : in.name().trim();
        if (name.length() < 2 || name.length() > 50) {
            errors.put("name", "Use 2 to 50 characters.");
        } else if (AdminCatalog.slug(name).isEmpty()) {
            errors.put("name", "Use English letters too, so the collection gets a web address.");
        }
        String tagline = in.tagline() == null || in.tagline().isBlank() ? null : in.tagline().trim();
        if (tagline != null && tagline.length() > 80) {
            errors.put("tagline", "Keep it under 80 characters.");
        }
        String description = in.description() == null || in.description().isBlank() ? null : in.description().trim();
        if (description != null && description.length() > 400) {
            errors.put("description", "Keep it under 400 characters.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        boolean active = in.active() == null || in.active();
        try {
            if (id == null) {
                int position = jdbc.sql("select coalesce(max(position), 0) + 1 from collections").query(Integer.class).single();
                jdbc.sql("""
                                insert into collections (name, slug, tagline, description, active, position)
                                values (:n, :s, :t, :d, :a, :p)""")
                        .param("n", name).param("s", AdminCatalog.slug(name)).param("t", tagline).param("d", description)
                        .param("a", active).param("p", position).update();
            } else if (jdbc.sql("update collections set name = :n, tagline = :t, description = :d, active = :a where id = :id")
                    .param("n", name).param("t", tagline).param("d", description).param("a", active).param("id", id)
                    .update() == 0) {
                throw ApiException.notFound("collection");
            }
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "collection.taken", "Please check the highlighted fields.",
                    Map.of("name", "Another collection already has this name."));
        }
        return collections();
    }

    /** Moves a collection one place up (-1) or down (+1); the home page shows them in this order. */
    @Transactional
    public List<CollectionView> moveCollection(long id, int direction) {
        List<Long> ids = new java.util.ArrayList<>(jdbc.sql("select id from collections order by position, id")
                .query(Long.class).list());
        int at = ids.indexOf(id);
        if (at < 0) {
            throw ApiException.notFound("collection");
        }
        int to = at + Integer.signum(direction);
        if (to >= 0 && to < ids.size()) {
            ids.remove(at);
            ids.add(to, id);
            for (int i = 0; i < ids.size(); i++) {
                jdbc.sql("update collections set position = :p where id = :id").param("p", i + 1).param("id", ids.get(i)).update();
            }
        }
        return collections();
    }

    @Transactional
    public List<CollectionView> setCollectionImage(long id, byte[] bytes) {
        if (jdbc.sql("select count(*) from collections where id = :id").param("id", id).query(Integer.class).single() == 0) {
            throw ApiException.notFound("collection");
        }
        String url = pictures.store("collections", bytes);
        jdbc.sql("update collections set image_url = :u where id = :id").param("u", url).param("id", id).update();
        return collections();
    }

    /** Texts sent or waiting, newest first. Codes show masked once sent. */
    public TextPage texts(String status, String phone, int page) {
        String where = "true";
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null && List.of("QUEUED", "SENDING", "SENT", "FAILED").contains(status)) {
            where += " and m.status = :status";
            params.put("status", status);
        }
        if (phone != null && !phone.isBlank()) {
            where += " and m.phone like :phone";
            params.put("phone", "%" + phone.trim() + "%");
        }
        int size = 50;
        long total = jdbc.sql("select count(*) from sms_messages m where " + where).params(params).query(Long.class).single();
        Map<String, Object> paged = new LinkedHashMap<>(params);
        paged.put("limit", size);
        paged.put("offset", (long) page * size);
        List<Text> items = jdbc.sql("""
                        select m.id, m.phone, m.kind,
                               case when m.sensitive and m.status <> 'SENT' then regexp_replace(m.body, '[0-9]{6}', '••••••', 'g')
                                    else m.body end,
                               m.status, m.attempts, m.provider, m.error, o.number, m.created_at, m.sent_at
                          from sms_messages m left join orders o on o.id = m.order_id
                         where %s order by m.created_at desc, m.id desc limit :limit offset :offset""".formatted(where))
                .params(paged)
                .query((rs, n) -> new Text(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getTimestamp(10).toInstant(),
                        instant(rs.getTimestamp(11))))
                .list();
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbc.sql("""
                        select status, count(*) from sms_messages where created_at > now() - interval '24 hours'
                         group by status""")
                .query(rs -> {
                    counts.put(rs.getString(1), rs.getInt(2));
                });
        return new TextPage(items, page, size, total, counts);
    }

    /** Sends a failed text again. */
    @Transactional
    public void retryText(long id) {
        if (jdbc.sql("update sms_messages set status = 'QUEUED', attempts = 0, error = null where id = :id and status = 'FAILED'")
                .param("id", id).update() == 0) {
            throw ApiException.conflict("text.not_failed", "Only failed texts can be sent again.");
        }
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
