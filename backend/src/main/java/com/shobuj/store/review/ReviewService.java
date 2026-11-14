package com.shobuj.store.review;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.Pictures;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reviews. Only a customer who received the plant (a delivered order of theirs holds it) can review
 * it, once, and change or delete their review later. Reviews show at once; staff can hide one or
 * reply to it. Each product keeps its average and count of published reviews.
 */
@Service
public class ReviewService {

    public static final int MAX_PHOTOS = 3;
    static final int PAGE_SIZE = 6;

    public record Photo(long id, String url) {
    }

    public record ReviewView(long id, int rating, String title, String body, String author, String bought,
                             List<Photo> photos, String reply, Instant repliedAt, Instant createdAt, boolean edited) {
    }

    /** {@code stars} counts reviews with 5, 4, 3, 2 and 1 stars, in that order. */
    public record Summary(Double average, int count, int[] stars, int withPhotos) {
    }

    public record Page(Summary summary, List<ReviewView> items, int page, int pageSize, long total) {
    }

    /** A customer's own review, wherever it is shown. */
    public record Mine(long id, String slug, String product, String image, int rating, String title, String body,
                       String bought, List<Photo> photos, String status, String reply, Instant createdAt) {
    }

    /** A delivered plant the customer hasn't reviewed yet. */
    public record ToReview(String slug, String product, String image, String bought, Instant deliveredAt) {
    }

    public record AccountReviews(List<ToReview> toReview, List<Mine> mine) {
    }

    /** What the product page needs for the signed-in shopper. */
    public record ForProduct(boolean canReview, Mine review) {
    }

    public record Input(int rating, String title, String body) {
    }

    public enum Sort {
        RECENT("r.created_at desc, r.id desc"),
        HIGHEST("r.rating desc, r.created_at desc, r.id desc"),
        LOWEST("r.rating asc, r.created_at desc, r.id desc"),
        PHOTOS("(select count(*) from review_photos ph where ph.review_id = r.id) > 0 desc, r.created_at desc, r.id desc");

        final String orderBy;

        Sort(String orderBy) {
            this.orderBy = orderBy;
        }

        static Sort parse(String s) {
            for (Sort v : values()) {
                if (v.name().equalsIgnoreCase(s)) {
                    return v;
                }
            }
            return RECENT;
        }
    }

    public record AdminReview(long id, String slug, String product, String image, int rating, String title, String body,
                              String author, String phone, String bought, String orderNumber, List<Photo> photos,
                              String status, String reply, Instant repliedAt, String repliedBy, Instant createdAt) {
    }

    public record AdminPage(List<AdminReview> items, int page, int pageSize, long total, Map<String, Long> counts) {
    }

    private final JdbcClient jdbc;
    private final Pictures pictures;

    ReviewService(JdbcClient jdbc, Pictures pictures) {
        this.jdbc = jdbc;
        this.pictures = pictures;
    }

    // ---- The shop ----

    public Page page(String slug, String sort, int page) {
        long productId = activeProduct(slug);
        Sort s = Sort.parse(sort);
        int p = Math.max(0, page);
        long total = jdbc.sql("select count(*) from reviews where product_id = :p and status = 'PUBLISHED'")
                .param("p", productId).query(Long.class).single();
        List<ReviewView> items = jdbc.sql("""
                        select r.id, r.rating, r.title, r.body, r.author, r.bought, r.reply, r.replied_at, r.created_at,
                               r.updated_at > r.created_at + interval '1 minute'
                          from reviews r
                         where r.product_id = :p and r.status = 'PUBLISHED'
                         order by %s limit :limit offset :offset""".formatted(s.orderBy))
                .param("p", productId).param("limit", PAGE_SIZE).param("offset", (long) p * PAGE_SIZE)
                .query((rs, n) -> new ReviewView(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), List.of(), rs.getString(7), instant(rs, 8), instant(rs, 9),
                        rs.getBoolean(10)))
                .list();
        Map<Long, List<Photo>> photos = photos(items.stream().map(ReviewView::id).toList());
        items = items.stream().map(r -> new ReviewView(r.id(), r.rating(), r.title(), r.body(), r.author(), r.bought(),
                photos.getOrDefault(r.id(), List.of()), r.reply(), r.repliedAt(), r.createdAt(), r.edited())).toList();
        return new Page(summary(productId), items, p, PAGE_SIZE, total);
    }

    Summary summary(long productId) {
        int[] stars = new int[5];
        jdbc.sql("select rating, count(*) from reviews where product_id = :p and status = 'PUBLISHED' group by rating")
                .param("p", productId)
                .query(rs -> {
                    stars[5 - rs.getInt(1)] = rs.getInt(2);
                });
        int count = 0;
        int sum = 0;
        for (int i = 0; i < 5; i++) {
            count += stars[i];
            sum += stars[i] * (5 - i);
        }
        int withPhotos = jdbc.sql("""
                        select count(*) from reviews r where r.product_id = :p and r.status = 'PUBLISHED'
                           and exists (select 1 from review_photos ph where ph.review_id = r.id)""")
                .param("p", productId).query(Integer.class).single();
        Double average = count == 0 ? null : Math.round(sum * 10.0 / count) / 10.0;
        return new Summary(average, count, stars, withPhotos);
    }

    // ---- A customer's own reviews ----

    public AccountReviews account(long customerId) {
        List<ToReview> toReview = jdbc.sql("""
                        select * from (
                            select distinct on (p.id) p.slug, p.name,
                                   (select url from product_images where product_id = p.id order by position, id limit 1),
                                   i.size || ' · ' || i.planter, o.delivered_at
                              from orders o join order_items i on i.order_id = o.id join products p on p.id = i.product_id
                             where o.customer_id = :c and o.status = 'DELIVERED' and p.status = 'ACTIVE'
                               and not exists (select 1 from reviews r where r.product_id = p.id and r.customer_id = :c)
                             order by p.id, o.delivered_at desc nulls last) t
                         order by t.delivered_at desc nulls last, t.name""")
                .param("c", customerId)
                .query((rs, n) -> new ToReview(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        instant(rs, 5)))
                .list();
        return new AccountReviews(toReview, mine("r.customer_id = :c", Map.of("c", customerId)));
    }

    public ForProduct forProduct(long customerId, String slug) {
        long productId = activeProduct(slug);
        Optional<Mine> review = mine("r.customer_id = :c and r.product_id = :p", Map.of("c", customerId, "p", productId))
                .stream().findFirst();
        return new ForProduct(review.isPresent() || delivered(customerId, productId).isPresent(), review.orElse(null));
    }

    /** Writes or rewrites the customer's review of a plant they received. */
    @Transactional
    public Mine save(long customerId, String slug, Input in) {
        long productId = activeProduct(slug);
        Map<String, String> errors = new LinkedHashMap<>();
        if (in.rating() < 1 || in.rating() > 5) {
            errors.put("rating", "Choose from 1 to 5 stars.");
        }
        String title = in.title() == null || in.title().isBlank() ? null : in.title().trim();
        String body = in.body() == null ? "" : in.body().trim();
        if (title != null && title.length() > 80) {
            errors.put("title", "Keep the headline under 80 characters.");
        }
        if (body.length() < 10) {
            errors.put("body", "Tell other shoppers a little more (at least 10 characters).");
        } else if (body.length() > 2000) {
            errors.put("body", "Reviews can be up to 2,000 characters.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        Optional<Long> existing = jdbc.sql("select id from reviews where customer_id = :c and product_id = :p")
                .param("c", customerId).param("p", productId).query(Long.class).optional();
        if (existing.isPresent()) {
            jdbc.sql("update reviews set rating = :r, title = :t, body = :b, updated_at = now() where id = :id")
                    .param("r", in.rating()).param("t", title).param("b", body).param("id", existing.get()).update();
        } else {
            Delivered d = delivered(customerId, productId).orElseThrow(() -> ApiException.conflict("review.not_bought",
                    "You can review a plant once it has been delivered to you."));
            String name = jdbc.sql("select name from customers where id = :c").param("c", customerId)
                    .query(String.class).single();
            jdbc.sql("""
                            insert into reviews (product_id, customer_id, order_id, rating, title, body, author, bought)
                            values (:p, :c, :o, :r, :t, :b, :a, :bought)""")
                    .param("p", productId).param("c", customerId).param("o", d.orderId()).param("r", in.rating())
                    .param("t", title).param("b", body).param("a", author(name)).param("bought", d.bought())
                    .update();
        }
        recount(productId);
        return mine("r.customer_id = :c and r.product_id = :p", Map.of("c", customerId, "p", productId)).get(0);
    }

    @Transactional
    public void delete(long customerId, long reviewId) {
        Long productId = jdbc.sql("delete from reviews where id = :id and customer_id = :c returning product_id")
                .param("id", reviewId).param("c", customerId).query(Long.class).optional()
                .orElseThrow(() -> ApiException.notFound("review"));
        recount(productId);
    }

    @Transactional
    public Mine addPhoto(long customerId, long reviewId, byte[] bytes) {
        ownReview(customerId, reviewId);
        int count = jdbc.sql("select count(*) from review_photos where review_id = :r").param("r", reviewId)
                .query(Integer.class).single();
        if (count >= MAX_PHOTOS) {
            throw ApiException.conflict("review.photos", "A review can have up to " + MAX_PHOTOS + " photos.");
        }
        String url = pictures.store("reviews", bytes);
        jdbc.sql("insert into review_photos (review_id, url, position) values (:r, :u, :p)")
                .param("r", reviewId).param("u", url).param("p", count).update();
        return mine("r.id = :id", Map.of("id", reviewId)).get(0);
    }

    @Transactional
    public Mine deletePhoto(long customerId, long reviewId, long photoId) {
        ownReview(customerId, reviewId);
        if (jdbc.sql("delete from review_photos where id = :p and review_id = :r").param("p", photoId).param("r", reviewId)
                .update() == 0) {
            throw ApiException.notFound("photo");
        }
        return mine("r.id = :id", Map.of("id", reviewId)).get(0);
    }

    // ---- Staff ----

    public AdminPage adminList(String filter, String q, int page) {
        String f = filter == null ? "ALL" : filter.toUpperCase();
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        switch (f) {
            case "PUBLISHED", "HIDDEN" -> {
                where.add("r.status = :s");
                params.put("s", f);
            }
            case "LOW" -> where.add("r.rating <= 2");
            case "UNANSWERED" -> where.add("r.reply is null and r.status = 'PUBLISHED'");
            case "PHOTOS" -> where.add("exists (select 1 from review_photos ph where ph.review_id = r.id)");
            default -> {
                // ALL
            }
        }
        if (q != null && !q.isBlank()) {
            where.add("(p.name ilike :q or r.author ilike :q or r.body ilike :q or c.phone like :q)");
            params.put("q", "%" + q.trim().replace("%", "") + "%");
        }
        String whereSql = where.isEmpty() ? "true" : String.join(" and ", where);
        String from = """
                reviews r join products p on p.id = r.product_id join customers c on c.id = r.customer_id
                left join orders o on o.id = r.order_id""";
        long total = jdbc.sql("select count(*) from " + from + " where " + whereSql).params(params)
                .query(Long.class).single();
        int pageSize = 20;
        int p = Math.max(0, page);
        Map<String, Object> all = new HashMap<>(params);
        all.put("limit", pageSize);
        all.put("offset", (long) p * pageSize);
        List<AdminReview> items = jdbc.sql("""
                        select r.id, p.slug, p.name,
                               (select url from product_images where product_id = p.id order by position, id limit 1),
                               r.rating, r.title, r.body, r.author, c.phone, r.bought, o.number, r.status, r.reply,
                               r.replied_at, r.replied_by, r.created_at
                          from %s where %s
                         order by r.created_at desc, r.id desc limit :limit offset :offset""".formatted(from, whereSql))
                .params(all)
                .query((rs, n) -> new AdminReview(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getString(10), rs.getString(11), List.of(), rs.getString(12), rs.getString(13),
                        instant(rs, 14), rs.getString(15), instant(rs, 16)))
                .list();
        Map<Long, List<Photo>> photos = photos(items.stream().map(AdminReview::id).toList());
        items = items.stream().map(r -> new AdminReview(r.id(), r.slug(), r.product(), r.image(), r.rating(), r.title(),
                r.body(), r.author(), r.phone(), r.bought(), r.orderNumber(), photos.getOrDefault(r.id(), List.of()),
                r.status(), r.reply(), r.repliedAt(), r.repliedBy(), r.createdAt())).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", jdbc.sql("select count(*) from reviews").query(Long.class).single());
        counts.put("UNANSWERED", jdbc.sql("select count(*) from reviews where reply is null and status = 'PUBLISHED'")
                .query(Long.class).single());
        counts.put("LOW", jdbc.sql("select count(*) from reviews where rating <= 2").query(Long.class).single());
        counts.put("PHOTOS", jdbc.sql("select count(distinct review_id) from review_photos").query(Long.class).single());
        counts.put("HIDDEN", jdbc.sql("select count(*) from reviews where status = 'HIDDEN'").query(Long.class).single());
        return new AdminPage(items, p, pageSize, total, counts);
    }

    /** Hides or shows a review, and sets or clears the shop's reply (null leaves either unchanged). */
    @Transactional
    public void moderate(long id, String status, String reply, String actor) {
        Long productId = jdbc.sql("select product_id from reviews where id = :id for update").param("id", id)
                .query(Long.class).optional().orElseThrow(() -> ApiException.notFound("review"));
        if (status != null) {
            if (!status.equals("PUBLISHED") && !status.equals("HIDDEN")) {
                throw ApiException.badRequest("review.status", "A review is either shown or hidden.");
            }
            jdbc.sql("update reviews set status = :s where id = :id").param("s", status).param("id", id).update();
        }
        if (reply != null) {
            String r = reply.trim();
            if (r.length() > 1000) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                        Map.of("reply", "Replies can be up to 1,000 characters."));
            }
            jdbc.sql("update reviews set reply = :r, replied_at = :at, replied_by = :by where id = :id")
                    .param("r", r.isEmpty() ? null : r)
                    .param("at", r.isEmpty() ? null : java.sql.Timestamp.from(Instant.now()))
                    .param("by", r.isEmpty() ? null : actor.replaceFirst("^staff:", "")).param("id", id).update();
        }
        recount(productId);
    }

    // ---- Helpers ----

    private record Delivered(long orderId, String bought) {
    }

    private Optional<Delivered> delivered(long customerId, long productId) {
        return jdbc.sql("""
                        select o.id, i.size || ' · ' || i.planter
                          from orders o join order_items i on i.order_id = o.id
                         where o.customer_id = :c and i.product_id = :p and o.status = 'DELIVERED'
                         order by o.delivered_at desc nulls last, o.id desc limit 1""")
                .param("c", customerId).param("p", productId)
                .query((rs, n) -> new Delivered(rs.getLong(1), rs.getString(2))).optional();
    }

    private List<Mine> mine(String where, Map<String, ?> params) {
        List<Mine> list = jdbc.sql("""
                        select r.id, p.slug, p.name,
                               (select url from product_images where product_id = p.id order by position, id limit 1),
                               r.rating, r.title, r.body, r.bought, r.status, r.reply, r.created_at
                          from reviews r join products p on p.id = r.product_id
                         where %s order by r.created_at desc, r.id desc""".formatted(where))
                .params(params)
                .query((rs, n) -> new Mine(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                        rs.getString(6), rs.getString(7), rs.getString(8), List.of(), rs.getString(9), rs.getString(10),
                        instant(rs, 11)))
                .list();
        Map<Long, List<Photo>> photos = photos(list.stream().map(Mine::id).toList());
        return list.stream().map(m -> new Mine(m.id(), m.slug(), m.product(), m.image(), m.rating(), m.title(), m.body(),
                m.bought(), photos.getOrDefault(m.id(), List.of()), m.status(), m.reply(), m.createdAt())).toList();
    }

    private Map<Long, List<Photo>> photos(List<Long> reviewIds) {
        Map<Long, List<Photo>> map = new HashMap<>();
        if (reviewIds.isEmpty()) {
            return map;
        }
        jdbc.sql("select review_id, id, url from review_photos where review_id in (:ids) order by position, id")
                .param("ids", reviewIds)
                .query(rs -> {
                    map.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(new Photo(rs.getLong(2), rs.getString(3)));
                });
        return map;
    }

    private void ownReview(long customerId, long reviewId) {
        if (jdbc.sql("select count(*) from reviews where id = :id and customer_id = :c").param("id", reviewId)
                .param("c", customerId).query(Long.class).single() == 0) {
            throw ApiException.notFound("review");
        }
    }

    private long activeProduct(String slug) {
        return jdbc.sql("select id from products where slug = :s and status = 'ACTIVE'").param("s", slug)
                .query(Long.class).optional().orElseThrow(() -> ApiException.notFound("product"));
    }

    /** Keeps the product's average and count in step with its published reviews. */
    void recount(long productId) {
        jdbc.sql("""
                        update products set
                               review_count = (select count(*) from reviews where product_id = :p and status = 'PUBLISHED'),
                               rating = (select round(avg(rating)::numeric, 1) from reviews where product_id = :p and status = 'PUBLISHED')
                         where id = :p""")
                .param("p", productId).update();
    }

    /** "Nadia Rahman" → "Nadia R."; one word stays as it is. */
    static String author(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].substring(0, 1).toUpperCase() + ".";
    }

    private static Instant instant(ResultSet rs, int column) throws SQLException {
        java.sql.Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }
}
