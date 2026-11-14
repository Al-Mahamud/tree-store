package com.shobuj.store.order;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.BangladeshPhone;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Reads orders for the people allowed to see them. */
@Service
public class OrderService {

    public record Item(String slug, String name, String planter, String size, String height, String image,
                       int unitPrice, int quantity, int lineTotal) {
    }

    public record Event(String status, String note, Instant at) {
    }

    public record OrderView(String number, String status, String paymentMethod, String paymentStatus,
                            Instant placedAt, String name, String phone, String email, String address, String area,
                            String city, String zone, String note, List<Item> items, int subtotal, int discount,
                            int shippingFee, int total, String couponCode, List<Event> events,
                            Instant paymentExpiresAt) {
    }

    /** One line in a customer's order history. {@code itemCount} counts plants, {@code lineCount} options. */
    public record Summary(String number, String status, Instant placedAt, int itemCount, int lineCount, int total,
                          List<String> images) {
    }

    public record History(List<Summary> items, int page, int pageSize, long total) {
    }

    /** What an earlier order held, for "order again". */
    public record PastItem(Long variantId, String name, String size, String planter, int quantity) {
    }

    private record Row(long id, String token, OrderView view) {
    }

    private final JdbcClient jdbc;

    OrderService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The thank-you page: whoever placed the order holds its access token. */
    public OrderView byToken(String number, String token) {
        Row row = row("o.number = :v", number).orElseThrow(() -> ApiException.notFound("order"));
        if (token == null || !MessageDigest.isEqual(row.token().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.notFound("order");
        }
        return row.view();
    }

    /** Guest tracking: the order number plus the phone number it was placed with. */
    public OrderView track(String number, String phone) {
        String normalised = BangladeshPhone.normalise(phone).orElseThrow(() -> ApiException.notFound("order"));
        Row row = row("o.number = :v", number == null ? "" : number.trim().toUpperCase())
                .filter(r -> r.view().phone().equals(normalised))
                .orElseThrow(() -> ApiException.notFound("order"));
        return row.view();
    }

    /** A signed-in customer's own order. */
    public OrderView forCustomer(long customerId, String number) {
        return row("o.number = :v", number == null ? "" : number.trim().toUpperCase())
                .filter(r -> ownedBy(r.id(), customerId))
                .orElseThrow(() -> ApiException.notFound("order"))
                .view();
    }

    public History history(long customerId, int page, int pageSize) {
        long total = jdbc.sql("select count(*) from orders where customer_id = :c").param("c", customerId)
                .query(Long.class).single();
        List<Summary> items = jdbc.sql("""
                        select o.number, o.status, o.created_at,
                               (select coalesce(sum(quantity), 0) from order_items where order_id = o.id),
                               (select count(*) from order_items where order_id = o.id), o.total,
                               array(select image_url from order_items where order_id = o.id and image_url is not null
                                      order by id limit 4)
                          from orders o where o.customer_id = :c
                         order by o.created_at desc, o.id desc limit :limit offset :offset""")
                .param("c", customerId).param("limit", pageSize).param("offset", (long) page * pageSize)
                .query((rs, n) -> new Summary(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant(),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6), List.of((String[]) rs.getArray(7).getArray())))
                .list();
        return new History(items, page, pageSize, total);
    }

    public List<PastItem> itemsForReorder(long customerId, String number) {
        OrderView order = forCustomer(customerId, number);
        return jdbc.sql("""
                        select i.variant_id, i.product_name, i.size, i.planter, i.quantity
                          from order_items i join orders o on o.id = i.order_id
                         where o.number = :n and o.customer_id = :c order by i.id""")
                .param("n", order.number()).param("c", customerId)
                .query((rs, n) -> new PastItem((Long) rs.getObject(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5)))
                .list();
    }

    /**
     * The id of an order the caller may act on: whoever holds its access token (the link on the
     * thank-you page), or the signed-in customer who placed it.
     */
    public long idFor(String number, String token) {
        Row row = row("o.number = :v", number == null ? "" : number.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("order"));
        boolean tokenOk = token != null && MessageDigest.isEqual(row.token().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
        boolean owner = com.shobuj.store.shared.CurrentCustomer.id().map(c -> ownedBy(row.id(), c)).orElse(false);
        if (!tokenOk && !owner) {
            throw ApiException.notFound("order");
        }
        return row.id();
    }

    private boolean ownedBy(long orderId, long customerId) {
        return jdbc.sql("select count(*) from orders where id = :id and customer_id = :c")
                .param("id", orderId).param("c", customerId).query(Long.class).single() > 0;
    }

    public Optional<OrderView> byId(long id) {
        return row("o.id = :v", id).map(Row::view);
    }

    private Optional<Row> row(String where, Object value) {
        Optional<long[]> id = jdbc.sql("select o.id from orders o where " + where).param("v", value)
                .query((rs, n) -> new long[] {rs.getLong(1)}).optional();
        if (id.isEmpty()) {
            return Optional.empty();
        }
        long orderId = id.get()[0];
        List<Item> items = jdbc.sql("""
                        select product_slug, product_name, planter, size, height, image_url, unit_price, quantity
                          from order_items where order_id = :id order by id""")
                .param("id", orderId)
                .query((rs, n) -> new Item(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getInt(7) * rs.getInt(8)))
                .list();
        List<Event> events = jdbc.sql("select status, note, created_at from order_events where order_id = :id order by created_at, id")
                .param("id", orderId)
                .query((rs, n) -> new Event(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant()))
                .list();
        return jdbc.sql("""
                        select access_token, number, status, payment_method, payment_status, created_at, name, phone,
                               email, address, area, city, zone, note, subtotal, discount, shipping_fee, total, coupon_code,
                               payment_expires_at
                          from orders where id = :id""")
                .param("id", orderId)
                .query((rs, n) -> new Row(orderId, rs.getString(1), new OrderView(rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getTimestamp(6).toInstant(), rs.getString(7),
                        rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12),
                        rs.getString(13), rs.getString(14), items, rs.getInt(15), rs.getInt(16), rs.getInt(17),
                        rs.getInt(18), rs.getString(19), events,
                        rs.getTimestamp(20) == null ? null : rs.getTimestamp(20).toInstant())))
                .optional();
    }
}
