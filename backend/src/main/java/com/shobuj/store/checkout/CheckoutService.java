package com.shobuj.store.checkout;

import com.shobuj.store.checkout.CartService.Line;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import com.shobuj.store.shared.StoreProperties.Zone;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a bag into an order: checks stock, prices everything, takes the stock and empties the bag. */
@Service
public class CheckoutService {

    public enum PaymentMethod {
        COD, ONLINE
    }

    public record Customer(String name, String phone, String email, String address, String area, String city,
                           Zone zone, String note) {
    }

    /** {@code problem} explains why the order cannot go to this zone, or is null. */
    public record Quote(int subtotal, int discount, int shippingFee, int total, String couponCode, String problem) {
    }

    public record Placed(long orderId, String number, String accessToken, int total, PaymentMethod paymentMethod) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;
    private final CartService carts;
    private final Coupons coupons;
    private final StoreProperties properties;
    private final Clock clock;

    CheckoutService(JdbcClient jdbc, CartService carts, Coupons coupons, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.carts = carts;
        this.coupons = coupons;
        this.properties = properties;
        this.clock = clock;
    }

    /** Totals for a delivery zone. A coupon that no longer applies is left out rather than failing. */
    public Quote quote(UUID cartId, Zone zone) {
        List<Line> lines = carts.lines(cartId);
        int subtotal = lines.stream().filter(l -> l.problem() == null).mapToInt(Line::lineTotal).sum();
        String code = couponCode(cartId);
        Coupons.Applied coupon = null;
        if (code != null && subtotal > 0) {
            try {
                coupon = coupons.apply(code, subtotal, clock.instant());
            } catch (ApiException ignored) {
                // The bag shows the coupon problem; the quote simply leaves it out.
            }
        }
        Quote quote = price(subtotal, coupon, zone);
        String problem = zoneProblem(lines, zone);
        return problem == null ? quote : new Quote(quote.subtotal(), quote.discount(), quote.shippingFee(),
                quote.total(), quote.couponCode(), problem);
    }

    /** Large trees travel badly; they are delivered inside Dhaka only. */
    static String zoneProblem(List<Line> lines, Zone zone) {
        if (zone != Zone.OUTSIDE) {
            return null;
        }
        List<String> names = lines.stream().filter(Line::dhakaOnly).map(l -> l.name() + " (" + l.size() + ")").toList();
        return names.isEmpty() ? null
                : String.join(", ", names) + (names.size() == 1 ? " is" : " are")
                        + " delivered inside Dhaka only. Choose a smaller size or a Dhaka address.";
    }

    private Quote price(int subtotal, Coupons.Applied coupon, Zone zone) {
        int discount = coupon == null ? 0 : coupon.discount();
        int afterDiscount = subtotal - discount;
        boolean free = (coupon != null && coupon.freeShipping()) || afterDiscount >= properties.delivery().freeOver();
        int shipping = subtotal == 0 || free ? 0 : properties.delivery().fee(zone);
        return new Quote(subtotal, discount, shipping, afterDiscount + shipping, coupon == null ? null : coupon.code(),
                null);
    }

    @Transactional
    public Placed place(UUID cartId, Customer customer, PaymentMethod method, Long customerId) {
        List<Line> lines = carts.lines(cartId);
        if (lines.isEmpty()) {
            throw CartService.emptyCart();
        }
        String zoneProblem = zoneProblem(lines, customer.zone());
        if (zoneProblem != null) {
            throw new ApiException(HttpStatus.CONFLICT, "checkout.dhaka_only", zoneProblem);
        }
        // Lock the variants so two shoppers cannot both buy the last piece.
        Map<Long, Integer> stock = new HashMap<>();
        jdbc.sql("select id, stock from variants where id in (:ids) order by id for update")
                .param("ids", lines.stream().map(Line::variantId).toList())
                .query(rs -> {
                    stock.put(rs.getLong(1), rs.getInt(2));
                });
        List<Line> short_ = lines.stream()
                .filter(l -> l.problem() == CartService.Problem.UNAVAILABLE || stock.getOrDefault(l.variantId(), 0) < l.quantity())
                .toList();
        if (!short_.isEmpty()) {
            String which = short_.stream().map(l -> l.name() + " (" + l.size() + ", " + l.planter() + ")")
                    .collect(Collectors.joining(", "));
            throw new ApiException(HttpStatus.CONFLICT, "checkout.stock_changed",
                    "Some items are no longer available in the quantity you chose: " + which + ". Please review your bag.");
        }

        int subtotal = lines.stream().mapToInt(Line::lineTotal).sum();
        String code = couponCode(cartId);
        Coupons.Applied coupon = null;
        if (code != null) {
            coupon = coupons.apply(code, subtotal, clock.instant());
            coupons.recordUse(coupon.code());
        }
        Quote quote = price(subtotal, coupon, customer.zone());

        long number = jdbc.sql("select nextval('order_number_seq')").query(Long.class).single();
        String orderNumber = "TN" + number;
        String token = token();
        String status = method == PaymentMethod.COD ? "PLACED" : "PENDING_PAYMENT";
        long orderId = jdbc.sql("""
                        insert into orders (number, access_token, customer_id, name, phone, email, address, area, city,
                                            zone, note, subtotal, discount, shipping_fee, total, coupon_code,
                                            payment_method, status)
                        values (:number, :token, :customerId, :name, :phone, :email, :address, :area, :city, :zone,
                                :note, :subtotal, :discount, :shipping, :total, :coupon, :method, :status)
                        returning id""")
                .param("number", orderNumber).param("token", token).param("customerId", customerId)
                .param("name", customer.name()).param("phone", customer.phone()).param("email", customer.email())
                .param("address", customer.address()).param("area", customer.area()).param("city", customer.city())
                .param("zone", customer.zone().name()).param("note", customer.note())
                .param("subtotal", quote.subtotal()).param("discount", quote.discount())
                .param("shipping", quote.shippingFee()).param("total", quote.total())
                .param("coupon", quote.couponCode()).param("method", method.name()).param("status", status)
                .query(Long.class).single();

        for (Line line : lines) {
            jdbc.sql("""
                            insert into order_items (order_id, variant_id, product_id, product_slug, product_name, planter,
                                                     size, height, sku, image_url, unit_price, quantity)
                            values (:o, :v, :p, :slug, :name, :planter, :size, :height, :sku, :image, :price, :qty)""")
                    .param("o", orderId).param("v", line.variantId()).param("p", line.productId())
                    .param("slug", line.slug()).param("name", line.name()).param("planter", line.planter())
                    .param("size", line.size()).param("height", line.height()).param("sku", line.sku()).param("image", line.image())
                    .param("price", line.unitPrice()).param("qty", line.quantity())
                    .update();
            jdbc.sql("update variants set stock = stock - :q where id = :v")
                    .param("q", line.quantity()).param("v", line.variantId()).update();
            jdbc.sql("update products set sold_count = sold_count + :q where id = :p")
                    .param("q", line.quantity()).param("p", line.productId()).update();
        }
        jdbc.sql("insert into order_events (order_id, status, note, actor) values (:o, :s, :n, 'customer')")
                .param("o", orderId).param("s", status)
                .param("n", method == PaymentMethod.COD ? "Order placed, cash on delivery" : "Waiting for payment")
                .update();
        jdbc.sql("delete from carts where id = :id").param("id", cartId).update();
        return new Placed(orderId, orderNumber, token, quote.total(), method);
    }

    private String couponCode(UUID cartId) {
        return jdbc.sql("select coupon_code from carts where id = :id").param("id", cartId)
                .query(String.class).optional().orElse(null);
    }

    private static String token() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
