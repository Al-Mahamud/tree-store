package com.shobuj.store.payment;

import com.shobuj.store.order.OrderStock;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Online payment for orders. An online order waits in PENDING_PAYMENT, holding its stock and slot,
 * until the gateway confirms the payment (it becomes PLACED), the customer switches to cash on
 * delivery, or the hold runs out (it is cancelled and everything is given back).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public record Options(boolean online, long holdMinutes) {
    }

    private record OrderRow(long id, String number, String token, String status, String method, int total,
                            Instant expiresAt, String name, String phone, String email, String address, String area,
                            int itemCount) {
    }

    private record Attempt(long id, long orderId, String tranId, int amount, String status) {
    }

    private final JdbcClient jdbc;
    private final SslCommerzClient gateway;
    private final OrderStock stock;
    private final TransactionTemplate tx;
    private final StoreProperties.Payment settings;
    private final Clock clock;
    private final com.shobuj.store.notify.OrderTexts texts;

    PaymentService(JdbcClient jdbc, SslCommerzClient gateway, OrderStock stock, TransactionTemplate tx, StoreProperties properties,
                   Clock clock, com.shobuj.store.notify.OrderTexts texts) {
        this.texts = texts;
        this.jdbc = jdbc;
        this.gateway = gateway;
        this.stock = stock;
        this.tx = tx;
        this.settings = properties.payment();
        this.clock = clock;
    }

    public Options options() {
        return new Options(gateway.configured(), settings.expiry().toMinutes());
    }

    public boolean onlineAvailable() {
        return gateway.configured();
    }

    /** When a new online order stops holding its stock and slot. */
    public Instant holdUntil() {
        return clock.instant().plus(settings.expiry());
    }

    /**
     * Opens the gateway's payment page for an order awaiting payment. Each call is a new attempt
     * with its own tran_id ({@code HT10001-1}, {@code HT10001-2} ...).
     */
    public String start(long orderId) {
        Attempt attempt = tx.execute(status -> {
            OrderRow order = lockOrder(orderId);
            if (!order.status().equals("PENDING_PAYMENT")) {
                throw ApiException.conflict("payment.not_due", order.status().equals("CANCELLED")
                        ? "This order was cancelled because it wasn't paid in time."
                        : "This order doesn't need paying online.");
            }
            if (order.expiresAt() != null && !order.expiresAt().isAfter(clock.instant())) {
                throw ApiException.conflict("payment.expired", "The time to pay for this order has run out.");
            }
            int tries = jdbc.sql("select count(*) from payment_attempts where order_id = :o").param("o", orderId)
                    .query(Integer.class).single();
            if (tries >= settings.maxAttempts()) {
                throw ApiException.conflict("payment.too_many_attempts",
                        "That's too many payment attempts. Please choose cash on delivery or contact us.");
            }
            String tranId = order.number() + "-" + (tries + 1);
            long id = jdbc.sql("insert into payment_attempts (order_id, tran_id, amount) values (:o, :t, :a) returning id")
                    .param("o", orderId).param("t", tranId).param("a", order.total()).query(Long.class).single();
            return new Attempt(id, orderId, tranId, order.total(), "PENDING");
        });
        OrderRow order = orderRow(orderId).orElseThrow();
        String back = gateway.callbackUrl() + "/api/v1/payments/sslcommerz";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("total_amount", attempt.amount() + ".00");
        fields.put("currency", "BDT");
        fields.put("tran_id", attempt.tranId());
        fields.put("success_url", back + "/return/success");
        fields.put("fail_url", back + "/return/fail");
        fields.put("cancel_url", back + "/return/cancel");
        fields.put("ipn_url", back + "/ipn");
        fields.put("cus_name", order.name());
        fields.put("cus_email", order.email() == null ? "customer@shobuj.example" : order.email());
        fields.put("cus_phone", order.phone());
        fields.put("cus_add1", order.address() + ", " + order.area());
        fields.put("cus_city", order.area().substring(order.area().lastIndexOf(", ") + 2));
        fields.put("cus_country", "Bangladesh");
        fields.put("shipping_method", "NO");
        fields.put("num_of_item", String.valueOf(order.itemCount()));
        fields.put("product_name", "Plants (order " + order.number() + ")");
        fields.put("product_category", "Plants");
        fields.put("product_profile", "general");
        fields.put("value_a", order.number());
        SslCommerzClient.Session session;
        try {
            session = gateway.session(fields);
        } catch (RuntimeException e) {
            log.warn("Opening a payment page for {} failed", attempt.tranId(), e);
            session = new SslCommerzClient.Session(null, "unreachable");
        }
        if (session.gatewayUrl() == null) {
            log.warn("SSLCommerz refused session {}: {}", attempt.tranId(), session.failedReason());
            setAttempt(attempt.tranId(), "FAILED", null);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "payment.gateway",
                    "We couldn't open the payment page. Please try again, or choose cash on delivery.");
        }
        return session.gatewayUrl();
    }

    /**
     * Applies a payment the gateway has confirmed. Safe to call more than once for the same payment
     * (IPN, the browser's return and the background check can all report it).
     */
    public boolean confirm(String tranId, SslCommerzClient.Payment payment) {
        if (!payment.paid() || !tranId.equals(payment.tranId())) {
            return false;
        }
        return Boolean.TRUE.equals(tx.execute(status -> {
            Optional<Attempt> found = lockAttempt(tranId);
            if (found.isEmpty()) {
                log.warn("Payment for unknown tran_id {}", tranId);
                return false;
            }
            Attempt attempt = found.get();
            if (attempt.status().equals("PAID")) {
                return true;
            }
            if (!"BDT".equals(payment.currency()) || payment.amount().compareTo(BigDecimal.valueOf(attempt.amount())) != 0) {
                log.error("Payment {} doesn't match: {} {} for an attempt of BDT {}", tranId, payment.currency(),
                        payment.amount(), attempt.amount());
                return false;
            }
            OrderRow order = lockOrder(attempt.orderId());
            jdbc.sql("""
                            update payment_attempts set status = 'PAID', val_id = :v, bank_tran_id = :b, card_type = :c,
                                                        updated_at = now()
                             where id = :id""")
                    .param("v", payment.valId()).param("b", payment.bankTranId()).param("c", payment.cardType())
                    .param("id", attempt.id()).update();
            String how = payment.cardType().isBlank() ? "online" : payment.cardType();
            switch (order.status()) {
                case "PENDING_PAYMENT" -> {
                    jdbc.sql("""
                                    update orders set status = 'PLACED', payment_status = 'PAID', paid_at = now(),
                                                      payment_expires_at = null, updated_at = now()
                                     where id = :id""")
                            .param("id", order.id()).update();
                    event(order.id(), "PLACED", "Paid ৳" + attempt.amount() + " by " + how);
                    texts.placed(order.id());
                }
                case "CANCELLED" -> {
                    jdbc.sql("update orders set payment_status = 'PAID', paid_at = now(), updated_at = now() where id = :id")
                            .param("id", order.id()).update();
                    event(order.id(), "CANCELLED", "Payment of ৳" + attempt.amount() + " arrived after the order was "
                            + "cancelled. Refund it.");
                    log.error("Order {} was paid after it was cancelled: refund {}", order.number(), tranId);
                    texts.cancelled(order.id(), "STAFF");
                }
                default -> {
                    // Switched to cash on delivery, then paid anyway: the rider collects nothing.
                    jdbc.sql("""
                                    update orders set payment_method = 'ONLINE', payment_status = 'PAID', paid_at = now(),
                                                      updated_at = now()
                                     where id = :id""")
                            .param("id", order.id()).update();
                    event(order.id(), order.status(), "Paid ৳" + attempt.amount() + " by " + how + "; nothing to collect");
                }
            }
            return true;
        }));
    }

    /** The gateway says this attempt failed or was cancelled. The order keeps waiting until it expires. */
    public void failed(String tranId, String gatewayStatus) {
        setAttempt(tranId, "CANCELLED".equalsIgnoreCase(gatewayStatus) ? "CANCELLED" : "FAILED", "PENDING");
    }

    /** The customer gives up on paying online: the order is placed as cash on delivery. */
    public void switchToCash(long orderId) {
        tx.executeWithoutResult(status -> {
            OrderRow order = lockOrder(orderId);
            if (!order.status().equals("PENDING_PAYMENT")) {
                throw ApiException.conflict("payment.not_due", order.status().equals("CANCELLED")
                        ? "This order was cancelled because it wasn't paid in time."
                        : "This order is already confirmed.");
            }
            jdbc.sql("""
                            update orders set status = 'PLACED', payment_method = 'COD', payment_expires_at = null,
                                              updated_at = now()
                             where id = :id""")
                    .param("id", orderId).update();
            event(orderId, "PLACED", "Changed to cash on delivery");
            texts.placed(orderId);
        });
    }

    /**
     * The background check. First asks the gateway about attempts that have waited a while (an IPN
     * can be lost, and never reaches a local run), then cancels orders whose time to pay has run out.
     */
    public void checkPending() {
        List<String> waiting = jdbc.sql("""
                        select a.tran_id from payment_attempts a join orders o on o.id = a.order_id
                         where a.status = 'PENDING' and o.status in ('PENDING_PAYMENT', 'CANCELLED')
                           and a.created_at < :since
                         order by a.created_at limit 20""")
                .param("since", Timestamp.from(clock.instant().minusSeconds(120)))
                .query(String.class).list();
        for (String tranId : waiting) {
            ask(tranId);
        }
        List<Long> expired = jdbc.sql("""
                        select id from orders where status = 'PENDING_PAYMENT' and payment_expires_at < :now
                         order by payment_expires_at limit 50""")
                .param("now", Timestamp.from(clock.instant()))
                .query(Long.class).list();
        for (long orderId : expired) {
            // A payment made just before the deadline still counts.
            jdbc.sql("select tran_id from payment_attempts where order_id = :o and status = 'PENDING'").param("o", orderId)
                    .query(String.class).list().forEach(this::ask);
            expire(orderId);
        }
    }

    private void ask(String tranId) {
        if (!gateway.configured()) {
            return;
        }
        try {
            gateway.query(tranId).stream().filter(SslCommerzClient.Payment::paid).findFirst()
                    .ifPresent(p -> confirm(tranId, p));
        } catch (RuntimeException e) {
            log.warn("Asking SSLCommerz about {} failed", tranId, e);
        }
    }

    private void expire(long orderId) {
        tx.executeWithoutResult(status -> {
            OrderRow order = lockOrder(orderId);
            if (!order.status().equals("PENDING_PAYMENT") || order.expiresAt() == null
                    || order.expiresAt().isAfter(clock.instant())) {
                return;
            }
            jdbc.sql("update orders set status = 'CANCELLED', payment_expires_at = null, updated_at = now() where id = :id")
                    .param("id", orderId).update();
            jdbc.sql("update payment_attempts set status = 'CANCELLED', updated_at = now() where order_id = :o and status = 'PENDING'")
                    .param("o", orderId).update();
            stock.release(orderId);
            event(orderId, "CANCELLED", "Not paid in time");
            texts.cancelled(orderId, "EXPIRED");
        });
    }

    private void setAttempt(String tranId, String status, String onlyIf) {
        jdbc.sql("update payment_attempts set status = :s, updated_at = now() where tran_id = :t"
                        + (onlyIf == null ? "" : " and status = '" + onlyIf + "'"))
                .param("s", status).param("t", tranId).update();
    }

    private void event(long orderId, String status, String note) {
        jdbc.sql("insert into order_events (order_id, status, note, actor) values (:o, :s, :n, 'payment')")
                .param("o", orderId).param("s", status).param("n", note).update();
    }

    private Optional<Attempt> lockAttempt(String tranId) {
        return jdbc.sql("select id, order_id, tran_id, amount, status from payment_attempts where tran_id = :t for update")
                .param("t", tranId)
                .query((rs, n) -> new Attempt(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5)))
                .optional();
    }

    private OrderRow lockOrder(long orderId) {
        jdbc.sql("select id from orders where id = :id for update").param("id", orderId).query(Long.class).single();
        return orderRow(orderId).orElseThrow();
    }

    private Optional<OrderRow> orderRow(long orderId) {
        return jdbc.sql("""
                        select o.id, o.number, o.access_token, o.status, o.payment_method, o.total, o.payment_expires_at,
                               o.name, o.phone, o.email, o.address, o.area || ', ' || o.city,
                               (select coalesce(sum(quantity), 0) from order_items where order_id = o.id)
                          from orders o where o.id = :id""")
                .param("id", orderId)
                .query((rs, n) -> new OrderRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getInt(6),
                        rs.getTimestamp(7) == null ? null : rs.getTimestamp(7).toInstant(), rs.getString(8),
                        rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getInt(13)))
                .optional();
    }
}
