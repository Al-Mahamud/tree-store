package com.shobuj.store.admin;

import com.shobuj.store.notify.CareTexts;
import com.shobuj.store.notify.OrderTexts;
import com.shobuj.store.order.OrderService;
import com.shobuj.store.order.OrderService.OrderView;
import com.shobuj.store.order.OrderStock;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orders as the shop sees them: the queue, status changes, cancellations, refunds and packing slips. */
@Service
public class AdminOrders {

    /** The shop's day starts and ends in Dhaka time. */
    static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");

    /**
     * What the next steps of an order can be. A shipped order can still be cancelled when the
     * courier brings it back. Cash orders are marked paid when delivered.
     */
    static final Map<String, List<String>> NEXT = Map.of(
            "PENDING_PAYMENT", List.of("CANCELLED"),
            "PLACED", List.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", List.of("SHIPPED", "CANCELLED"),
            "SHIPPED", List.of("DELIVERED", "CANCELLED"),
            "DELIVERED", List.of(),
            "CANCELLED", List.of());

    static final Set<String> FILTERS = Set.of("OPEN", "ALL", "REFUND_DUE", "PENDING_PAYMENT", "PLACED", "CONFIRMED",
            "SHIPPED", "DELIVERED", "CANCELLED");

    static final Set<String> ZONES = Set.of("DHAKA", "OUTSIDE");

    public record Row(String number, String status, String paymentMethod, String paymentStatus, Instant placedAt,
                      String name, String phone, String area, String city, String zone, boolean dhakaOnly,
                      int itemCount, int total) {
    }

    public record Page(List<Row> items, int page, int pageSize, long total, Map<String, Integer> counts) {
    }

    public record Attempt(String tranId, int amount, String status, String cardType, String bankTranId, Instant at) {
    }

    public record Detail(OrderView order, List<String> next, List<Attempt> attempts, Instant refundedAt,
                         String refundReference, boolean refundDue, Long customerId, int customerOrders,
                         List<String> skus) {
    }

    public record DayTotal(LocalDate date, int orders, int revenue) {
    }

    public record TopPlant(String name, String slug, String image, int quantity, int revenue) {
    }

    public record Dashboard(int ordersToday, int revenueToday, int averageOrder, Map<String, Integer> open,
                            int refundsDue, int lowStock, int awaitingPayment, List<DayTotal> week,
                            List<TopPlant> topPlants, List<Row> recent) {
    }

    private static final String ROW_SELECT = """
            select o.number, o.status, o.payment_method, o.payment_status, o.created_at, o.name, o.phone, o.area,
                   o.city, o.zone,
                   exists (select 1 from order_items i join variants v on v.id = i.variant_id
                            where i.order_id = o.id and v.dhaka_only),
                   (select coalesce(sum(quantity), 0) from order_items where order_id = o.id), o.total
              from orders o""";

    private final JdbcClient jdbc;
    private final OrderService orders;
    private final OrderStock stock;
    private final StoreProperties properties;
    private final Clock clock;

    private final OrderTexts texts;
    private final CareTexts care;

    AdminOrders(JdbcClient jdbc, OrderService orders, OrderStock stock, StoreProperties properties, Clock clock,
                OrderTexts texts, CareTexts care) {
        this.texts = texts;
        this.care = care;
        this.jdbc = jdbc;
        this.orders = orders;
        this.stock = stock;
        this.properties = properties;
        this.clock = clock;
    }

    public Page list(String filter, String zone, String q, int page) {
        String f = filter == null || !FILTERS.contains(filter) ? "OPEN" : filter;
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        switch (f) {
            case "ALL" -> where.add("true");
            case "OPEN" -> where.add("o.status in ('PLACED', 'CONFIRMED', 'SHIPPED')");
            case "REFUND_DUE" -> where.add("o.status = 'CANCELLED' and o.payment_status = 'PAID'");
            default -> {
                where.add("o.status = :status");
                params.put("status", f);
            }
        }
        if (zone != null && ZONES.contains(zone)) {
            where.add("o.zone = :zone");
            params.put("zone", zone);
        }
        if (q != null && !q.isBlank()) {
            where.add("(o.number ilike :q or o.phone like :q or o.name ilike :q)");
            params.put("q", "%" + q.trim() + "%");
        }
        String condition = String.join(" and ", where);
        // Open orders oldest first, so nothing waits too long; everything else newest first.
        String order = Set.of("OPEN", "PLACED", "CONFIRMED", "SHIPPED").contains(f) ? "o.created_at" : "o.created_at desc";
        int size = 25;
        long total = jdbc.sql("select count(*) from orders o where " + condition).params(params).query(Long.class).single();
        Map<String, Object> paged = new HashMap<>(params);
        paged.put("limit", size);
        paged.put("offset", (long) page * size);
        List<Row> items = jdbc.sql(ROW_SELECT + " where " + condition + " order by " + order + " limit :limit offset :offset")
                .params(paged).query((rs, n) -> row(rs)).list();
        return new Page(items, page, size, total, counts());
    }

    private Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : List.of("PENDING_PAYMENT", "PLACED", "CONFIRMED", "SHIPPED")) {
            counts.put(s, 0);
        }
        jdbc.sql("""
                        select status, count(*) from orders
                         where status in ('PENDING_PAYMENT', 'PLACED', 'CONFIRMED', 'SHIPPED') group by status""")
                .query(rs -> {
                    counts.put(rs.getString(1), rs.getInt(2));
                });
        counts.put("REFUND_DUE", refundsDue());
        return counts;
    }

    public Detail detail(String number) {
        long id = idOf(number);
        OrderView view = orders.byId(id).orElseThrow();
        List<Attempt> attempts = jdbc.sql("""
                        select tran_id, amount, status, card_type, bank_tran_id, created_at
                          from payment_attempts where order_id = :o order by created_at, id""")
                .param("o", id)
                .query((rs, n) -> new Attempt(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toInstant()))
                .list();
        record Extra(Instant refundedAt, String reference, Long customerId) {
        }
        Extra extra = jdbc.sql("select refunded_at, refund_reference, customer_id from orders where id = :id").param("id", id)
                .query((rs, n) -> new Extra(rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant(),
                        rs.getString(2), (Long) rs.getObject(3)))
                .single();
        int customerOrders = jdbc.sql("""
                        select count(*) from orders
                         where (customer_id = :c or phone = :p) and status <> 'CANCELLED'""")
                .param("c", extra.customerId()).param("p", view.phone()).query(Integer.class).single();
        List<String> skus = jdbc.sql("select sku from order_items where order_id = :o order by id").param("o", id)
                .query(String.class).list();
        boolean refundDue = view.status().equals("CANCELLED") && view.paymentStatus().equals("PAID");
        return new Detail(view, NEXT.getOrDefault(view.status(), List.of()), attempts, extra.refundedAt(),
                extra.reference(), refundDue, extra.customerId(), customerOrders, skus);
    }

    /**
     * Moves an order on. Shipping can record the courier and its tracking number. Cancelling gives
     * back stock (unless the plants can't be sold again), and a paid order then shows as a refund
     * due. A cash order is marked paid when it is delivered.
     */
    @Transactional
    public void changeStatus(String number, String target, String note, boolean restock, String courier,
                             String trackingCode, String actor) {
        long id = lock(number);
        String current = jdbc.sql("select status from orders where id = :id").param("id", id).query(String.class).single();
        if (!NEXT.getOrDefault(current, List.of()).contains(target)) {
            throw ApiException.conflict("order.status", "An order that is " + human(current)
                    + " can't be marked " + human(target) + ".");
        }
        jdbc.sql("update orders set status = :s, payment_expires_at = null, updated_at = now() where id = :id")
                .param("s", target).param("id", id).update();
        List<String> notes = new ArrayList<>();
        if (note != null && !note.isBlank()) {
            notes.add(note.trim());
        }
        if (target.equals("SHIPPED")) {
            String c = courier == null || courier.isBlank() ? null : courier.trim();
            String t = trackingCode == null || trackingCode.isBlank() ? null : trackingCode.trim();
            jdbc.sql("update orders set courier = :c, tracking_code = :t where id = :id")
                    .param("c", c).param("t", t).param("id", id).update();
            if (c != null) {
                notes.add("With " + c + (t == null ? "" : " (" + t + ")"));
            }
        }
        if (target.equals("CANCELLED")) {
            jdbc.sql("update payment_attempts set status = 'CANCELLED', updated_at = now() where order_id = :o and status = 'PENDING'")
                    .param("o", id).update();
            if (restock) {
                stock.release(id);
            } else {
                notes.add("Plants not returned to stock");
            }
            Integer paid = jdbc.sql("select total from orders where id = :id and payment_status = 'PAID'").param("id", id)
                    .query(Integer.class).optional().orElse(null);
            if (paid != null) {
                notes.add("Refund ৳" + paid + " due");
            }
        }
        if (target.equals("DELIVERED")) {
            int collected = jdbc.sql("""
                            update orders set payment_status = 'PAID', paid_at = now()
                             where id = :id and payment_method = 'COD' and payment_status = 'UNPAID'""")
                    .param("id", id).update();
            if (collected > 0) {
                notes.add("Cash collected");
            }
            Instant now = clock.instant();
            jdbc.sql("update orders set delivered_at = :at where id = :id")
                    .param("at", Timestamp.from(now)).param("id", id).update();
            care.schedule(id, now);
        }
        event(id, target, notes.isEmpty() ? null : String.join(". ", notes), actor);
        switch (target) {
            case "SHIPPED" -> texts.shipped(id);
            case "CANCELLED" -> texts.cancelled(id, "STAFF");
            default -> {
                // Confirming is done by phone, and the courier hands the plants over: neither is texted.
            }
        }
    }

    /** Records that a payment was refunded (in the SSLCommerz merchant panel), with its reference. */
    @Transactional
    public void refund(String number, String reference, String actor) {
        long id = lock(number);
        int updated = jdbc.sql("""
                        update orders set payment_status = 'REFUNDED', refunded_at = now(), refund_reference = :r,
                                          updated_at = now()
                         where id = :id and status = 'CANCELLED' and payment_status = 'PAID'""")
                .param("r", reference).param("id", id).update();
        if (updated == 0) {
            throw ApiException.conflict("order.refund", "Only cancelled orders that were paid can be refunded.");
        }
        event(id, "CANCELLED", "Refunded (" + reference + ")", actor);
    }

    @Transactional
    public void addNote(String number, String note, String actor) {
        long id = idOf(number);
        String status = jdbc.sql("select status from orders where id = :id").param("id", id).query(String.class).single();
        event(id, status, note.trim(), actor);
    }

    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(clock.withZone(DHAKA));
        Timestamp from = Timestamp.from(today.minusDays(6).atStartOfDay(DHAKA).toInstant());
        Map<LocalDate, DayTotal> byDay = new LinkedHashMap<>();
        for (int d = 6; d >= 0; d--) {
            byDay.put(today.minusDays(d), new DayTotal(today.minusDays(d), 0, 0));
        }
        jdbc.sql("""
                        select (created_at at time zone :zone)::date, count(*), coalesce(sum(total), 0)
                          from orders where created_at >= :from and status not in ('PENDING_PAYMENT', 'CANCELLED')
                         group by 1""")
                .param("zone", DHAKA.getId()).param("from", from)
                .query(rs -> {
                    LocalDate day = rs.getDate(1).toLocalDate();
                    byDay.computeIfPresent(day, (k, v) -> new DayTotal(k, safeInt(rs, 2), safeInt(rs, 3)));
                });
        DayTotal now = byDay.get(today);
        Map<String, Integer> open = counts();
        open.remove("REFUND_DUE");
        int awaiting = open.remove("PENDING_PAYMENT");
        int low = jdbc.sql("""
                        select count(*) from variants v join products p on p.id = v.product_id
                         where p.status = 'ACTIVE' and v.stock <= :low""")
                .param("low", properties.admin().lowStock()).query(Integer.class).single();
        Timestamp month = Timestamp.from(today.minusDays(29).atStartOfDay(DHAKA).toInstant());
        List<TopPlant> top = jdbc.sql("""
                        select i.product_name, i.product_slug, max(i.image_url), sum(i.quantity), sum(i.quantity * i.unit_price)
                          from order_items i join orders o on o.id = i.order_id
                         where o.created_at >= :from and o.status not in ('PENDING_PAYMENT', 'CANCELLED')
                         group by i.product_name, i.product_slug
                         order by 4 desc, 5 desc limit 5""")
                .param("from", month)
                .query((rs, n) -> new TopPlant(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5)))
                .list();
        List<Row> recent = jdbc.sql(ROW_SELECT + " order by o.created_at desc limit 8").query((rs, n) -> row(rs)).list();
        return new Dashboard(now.orders(), now.revenue(), now.orders() == 0 ? 0 : now.revenue() / now.orders(), open,
                refundsDue(), low, awaiting, List.copyOf(byDay.values()), top, recent);
    }

    private int refundsDue() {
        return jdbc.sql("select count(*) from orders where status = 'CANCELLED' and payment_status = 'PAID'")
                .query(Integer.class).single();
    }

    private static int safeInt(java.sql.ResultSet rs, int column) {
        try {
            return rs.getInt(column);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private long idOf(String number) {
        return jdbc.sql("select id from orders where number = :n").param("n", number == null ? "" : number.trim().toUpperCase())
                .query(Long.class).optional().orElseThrow(() -> ApiException.notFound("order"));
    }

    private long lock(String number) {
        long id = idOf(number);
        jdbc.sql("select id from orders where id = :id for update").param("id", id).query(Long.class).single();
        return id;
    }

    private void event(long orderId, String status, String note, String actor) {
        jdbc.sql("insert into order_events (order_id, status, note, actor) values (:o, :s, :n, :a)")
                .param("o", orderId).param("s", status).param("n", note).param("a", actor).update();
    }

    private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant(),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getBoolean(11),
                rs.getInt(12), rs.getInt(13));
    }

    private static String human(String status) {
        return switch (status) {
            case "PENDING_PAYMENT" -> "awaiting payment";
            case "PLACED" -> "placed";
            case "CONFIRMED" -> "confirmed";
            case "SHIPPED" -> "shipped";
            case "DELIVERED" -> "delivered";
            case "CANCELLED" -> "cancelled";
            default -> status.toLowerCase();
        };
    }
}
