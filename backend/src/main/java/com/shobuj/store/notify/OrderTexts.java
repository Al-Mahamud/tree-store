package com.shobuj.store.notify;

import com.shobuj.store.shared.StoreProperties;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The texts customers get about an order: received, sent out and cancelled. Plain English and "Tk"
 * rather than "৳", so each fits one standard 160-character SMS.
 */
@Component
public class OrderTexts {

    private record Order(long id, String number, String phone, int total, String paymentStatus, String courier,
                         String trackingCode) {

        boolean paid() {
            return paymentStatus.equals("PAID");
        }
    }

    private final JdbcClient jdbc;
    private final SmsOutbox outbox;
    private final String shop;

    OrderTexts(JdbcClient jdbc, SmsOutbox outbox, StoreProperties properties) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.shop = shopAddress(properties.webUrl());
    }

    static String shopAddress(String webUrl) {
        return webUrl.replaceFirst("^https?://", "").replaceFirst("^www\\.", "").replaceAll("/$", "");
    }

    static String taka(int amount) {
        return "Tk" + NumberFormat.getIntegerInstance(Locale.ENGLISH).format(amount);
    }

    /** A cash order placed, an online order paid, or an unpaid order switched to cash. */
    public void placed(long orderId) {
        order(orderId).ifPresent(o -> outbox.queue(o.phone(), "ORDER_PLACED",
                "Shobuj: Thanks! We got order " + o.number() + " (" + taka(o.total())
                        + (o.paid() ? ", paid" : ", cash on delivery") + "). We'll call to confirm before sending "
                        + "your plants. Track: " + shop + "/track",
                o.id(), false));
    }

    public void shipped(long orderId) {
        order(orderId).ifPresent(o -> {
            String with = o.courier() == null ? "with our delivery team"
                    : "with " + cut(o.courier(), 28) + (o.trackingCode() == null ? "" : " (" + cut(o.trackingCode(), 24) + ")");
            String pay = o.paid() ? "Nothing to pay at the door." : "Please keep " + taka(o.total()) + " ready.";
            outbox.queue(o.phone(), "ORDER_SHIPPED",
                    "Shobuj: Your plants (order " + o.number() + ") are on the way " + with + ". " + pay,
                    o.id(), false);
        });
    }

    /** {@code why}: STAFF, or EXPIRED when an online order wasn't paid in time. */
    public void cancelled(long orderId, String why) {
        order(orderId).ifPresent(o -> {
            String tail = o.paid() ? " Your payment of " + taka(o.total()) + " will be refunded within 7 days."
                    : why.equals("EXPIRED") ? " It wasn't paid in time, so nothing was charged."
                    : " Call us if this is a surprise.";
            outbox.queue(o.phone(), "ORDER_CANCELLED", "Shobuj: Order " + o.number() + " was cancelled." + tail,
                    o.id(), false);
        });
    }

    private Optional<Order> order(long id) {
        return jdbc.sql("select id, number, phone, total, payment_status, courier, tracking_code from orders where id = :id")
                .param("id", id)
                .query((rs, n) -> new Order(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)))
                .optional();
    }

    /** Shortens free text typed by staff. No "…": one character outside the SMS alphabet halves the length. */
    static String cut(String s, int max) {
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max).trim();
    }
}
