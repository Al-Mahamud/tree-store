package com.shobuj.store.checkout;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shopping bags. A bag is identified by a random id kept in a cookie. */
@Service
public class CartService {

    public static final int MAX_PER_LINE = 10;

    /** Why a line cannot be bought as it is. */
    public enum Problem {
        UNAVAILABLE, OUT_OF_STOCK, NOT_ENOUGH_STOCK
    }

    public record Line(long variantId, long productId, String slug, String name, String planter, String planterHex,
                       String size, String height, String sku, String image, int unitPrice, Integer compareAtPrice,
                       int quantity, int stock, int lineTotal, boolean dhakaOnly, Problem problem) {
    }

    public record CouponView(String code, String description, int discount, boolean freeShipping) {
    }

    public record CartView(List<Line> items, int itemCount, int subtotal, int discount, CouponView coupon,
                           String couponProblem, int total, int freeDeliveryOver, int remainingForFreeDelivery,
                           boolean dhakaOnly, boolean canCheckout) {
    }

    private final JdbcClient jdbc;
    private final Coupons coupons;
    private final StoreProperties properties;
    private final Clock clock;

    CartService(JdbcClient jdbc, Coupons coupons, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.coupons = coupons;
        this.properties = properties;
        this.clock = clock;
    }

    public CartView view(Optional<UUID> cartId) {
        if (cartId.isEmpty()) {
            return summarise(List.of(), null);
        }
        String code = jdbc.sql("select coupon_code from carts where id = :id").param("id", cartId.get())
                .query(String.class).optional().orElse(null);
        return summarise(lines(cartId.get()), code);
    }

    /** Adds to the bag (creating it if needed, owned by {@code customerId} when signed in) and returns the bag id. */
    @Transactional
    public UUID add(Optional<UUID> cartId, Long customerId, long variantId, int quantity) {
        if (quantity < 1 || quantity > MAX_PER_LINE) {
            throw ApiException.badRequest("cart.quantity", "Quantity must be between 1 and " + MAX_PER_LINE + ".");
        }
        Integer stock = jdbc.sql("""
                        select v.stock from variants v join products p on p.id = v.product_id
                         where v.id = :id and p.status = 'ACTIVE'""")
                .param("id", variantId).query(Integer.class).optional()
                .orElseThrow(() -> ApiException.notFound("variant"));
        UUID id = cartId.filter(c -> belongsTo(c, customerId)).orElseGet(() -> create(customerId));
        int already = jdbc.sql("select quantity from cart_items where cart_id = :c and variant_id = :v")
                .param("c", id).param("v", variantId).query(Integer.class).optional().orElse(0);
        int wanted = Math.min(already + quantity, MAX_PER_LINE);
        if (stock == 0) {
            throw ApiException.conflict("cart.out_of_stock", "Sorry, that option has just sold out.");
        }
        if (wanted > stock) {
            throw ApiException.conflict("cart.not_enough_stock",
                    "Only " + stock + " left in this option" + (already > 0 ? " and you already have " + already + " in your bag." : "."));
        }
        jdbc.sql("""
                        insert into cart_items (cart_id, variant_id, quantity) values (:c, :v, :q)
                        on conflict (cart_id, variant_id) do update set quantity = :q""")
                .param("c", id).param("v", variantId).param("q", wanted).update();
        touch(id);
        return id;
    }

    @Transactional
    public void setQuantity(UUID cartId, long variantId, int quantity) {
        if (quantity == 0) {
            remove(cartId, variantId);
            return;
        }
        if (quantity < 0 || quantity > MAX_PER_LINE) {
            throw ApiException.badRequest("cart.quantity", "Quantity must be between 0 and " + MAX_PER_LINE + ".");
        }
        int stock = jdbc.sql("select stock from variants where id = :v").param("v", variantId)
                .query(Integer.class).optional().orElseThrow(() -> ApiException.notFound("variant"));
        if (quantity > stock) {
            throw ApiException.conflict("cart.not_enough_stock", "Only " + stock + " left in this option.");
        }
        int updated = jdbc.sql("update cart_items set quantity = :q where cart_id = :c and variant_id = :v")
                .param("q", quantity).param("c", cartId).param("v", variantId).update();
        if (updated == 0) {
            throw ApiException.notFound("cart_item");
        }
        touch(cartId);
    }

    @Transactional
    public void remove(UUID cartId, long variantId) {
        jdbc.sql("delete from cart_items where cart_id = :c and variant_id = :v")
                .param("c", cartId).param("v", variantId).update();
        touch(cartId);
    }

    @Transactional
    public void applyCoupon(UUID cartId, String code) {
        int subtotal = lines(cartId).stream().filter(l -> l.problem() == null).mapToInt(Line::lineTotal).sum();
        Coupons.Applied applied = coupons.apply(code, subtotal, clock.instant());
        jdbc.sql("update carts set coupon_code = :code, updated_at = now() where id = :id")
                .param("code", applied.code()).param("id", cartId).update();
    }

    @Transactional
    public void removeCoupon(UUID cartId) {
        jdbc.sql("update carts set coupon_code = null, updated_at = now() where id = :id").param("id", cartId).update();
    }

    public List<Line> lines(UUID cartId) {
        return jdbc.sql("""
                        select v.id, p.id, p.slug, p.name, pl.name, pl.hex, v.size, v.height, v.sku,
                               (select pi.url from product_images pi
                                 where pi.product_id = p.id and (pi.planter_id = pl.id or pi.planter_id is null)
                                 order by pi.planter_id nulls last, pi.position limit 1),
                               v.price, v.compare_at_price, ci.quantity, v.stock, p.status, v.dhaka_only
                          from cart_items ci
                          join variants v on v.id = ci.variant_id
                          join products p on p.id = v.product_id
                          join planters pl on pl.id = v.planter_id
                         where ci.cart_id = :cart
                         order by ci.added_at, v.id""")
                .param("cart", cartId)
                .query((rs, n) -> {
                    int price = rs.getInt(11);
                    int quantity = rs.getInt(13);
                    int stock = rs.getInt(14);
                    Problem problem = !rs.getString(15).equals("ACTIVE") ? Problem.UNAVAILABLE
                            : stock == 0 ? Problem.OUT_OF_STOCK
                            : quantity > stock ? Problem.NOT_ENOUGH_STOCK : null;
                    String size = com.shobuj.store.catalog.Care.Size.valueOf(rs.getString(7)).label;
                    return new Line(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), size, rs.getString(8), rs.getString(9), rs.getString(10), price,
                            (Integer) rs.getObject(12), quantity, stock, price * quantity, rs.getBoolean(16), problem);
                })
                .list();
    }

    private CartView summarise(List<Line> lines, String couponCode) {
        int subtotal = lines.stream().filter(l -> l.problem() == null).mapToInt(Line::lineTotal).sum();
        int count = lines.stream().mapToInt(Line::quantity).sum();
        CouponView coupon = null;
        String couponProblem = null;
        if (couponCode != null && subtotal > 0) {
            try {
                Coupons.Applied applied = coupons.apply(couponCode, subtotal, clock.instant());
                coupon = new CouponView(applied.code(), applied.description(), applied.discount(), applied.freeShipping());
            } catch (ApiException e) {
                couponProblem = couponCode + ": " + e.getMessage();
            }
        }
        int discount = coupon == null ? 0 : coupon.discount();
        int total = subtotal - discount;
        int freeOver = properties.delivery().freeOver();
        boolean freeByCoupon = coupon != null && coupon.freeShipping();
        int remaining = freeByCoupon ? 0 : Math.max(0, freeOver - total);
        boolean canCheckout = !lines.isEmpty() && lines.stream().allMatch(l -> l.problem() == null);
        boolean dhakaOnly = lines.stream().anyMatch(Line::dhakaOnly);
        return new CartView(lines, count, subtotal, discount, coupon, couponProblem, total, freeOver, remaining,
                dhakaOnly, canCheckout);
    }

    /** The bag of a signed-in customer. */
    public Optional<UUID> ofCustomer(long customerId) {
        return jdbc.sql("select id from carts where customer_id = :c").param("c", customerId).query(UUID.class).optional();
    }

    /** True if the bag exists and belongs to this customer, or to no one when {@code customerId} is null. */
    public boolean belongsTo(UUID cartId, Long customerId) {
        return jdbc.sql("select count(*) from carts where id = :id and customer_id is not distinct from cast(:c as bigint)")
                .param("id", cartId).param("c", customerId).query(Long.class).single() > 0;
    }

    /**
     * At sign-in, the bag filled as a guest joins the customer's own bag. If both hold the same
     * plant option, the larger quantity wins (never above the per-line limit); a coupon carries over
     * if the customer's bag has none.
     */
    @Transactional
    public void claim(long customerId, Optional<UUID> guestCart) {
        Optional<UUID> guest = guestCart.filter(c -> belongsTo(c, null));
        if (guest.isEmpty()) {
            return;
        }
        Optional<UUID> own = ofCustomer(customerId);
        if (own.isEmpty()) {
            jdbc.sql("update carts set customer_id = :c, updated_at = now() where id = :id")
                    .param("c", customerId).param("id", guest.get()).update();
            return;
        }
        UUID target = own.get();
        jdbc.sql("""
                        insert into cart_items (cart_id, variant_id, quantity, added_at)
                        select :target, variant_id, quantity, added_at from cart_items where cart_id = :guest
                        on conflict (cart_id, variant_id) do update
                           set quantity = least(:max, greatest(cart_items.quantity, excluded.quantity))""")
                .param("target", target).param("guest", guest.get()).param("max", MAX_PER_LINE).update();
        jdbc.sql("""
                        update carts set coupon_code = coalesce(coupon_code, (select coupon_code from carts where id = :guest)),
                                         updated_at = now()
                         where id = :target""")
                .param("target", target).param("guest", guest.get()).update();
        jdbc.sql("delete from carts where id = :guest").param("guest", guest.get()).update();
    }

    private UUID create(Long customerId) {
        if (customerId == null) {
            UUID id = UUID.randomUUID();
            jdbc.sql("insert into carts (id) values (:id)").param("id", id).update();
            return id;
        }
        // One bag per customer, even if two devices add their first plant at the same moment.
        return jdbc.sql("""
                        insert into carts (id, customer_id) values (:id, :c)
                        on conflict (customer_id) where customer_id is not null do update set updated_at = now()
                        returning id""")
                .param("id", UUID.randomUUID()).param("c", customerId).query(UUID.class).single();
    }

    private void touch(UUID id) {
        jdbc.sql("update carts set updated_at = now() where id = :id").param("id", id).update();
    }

    static ApiException emptyCart() {
        return new ApiException(HttpStatus.CONFLICT, "cart.empty", "Your bag is empty.");
    }
}
