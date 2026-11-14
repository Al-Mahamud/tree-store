package com.shobuj.store.checkout;

import com.shobuj.store.shared.CurrentCustomer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Finds the bag of the current request: a signed-in customer's own bag, or a guest bag named by
 * the {@code shobuj_cart} cookie. Guests keep the cookie; signed-in customers don't need one.
 */
@Component
public class Bags {

    private final CartService carts;

    Bags(CartService carts) {
        this.carts = carts;
    }

    public Optional<UUID> current(HttpServletRequest request) {
        Optional<Long> customer = CurrentCustomer.id();
        if (customer.isPresent()) {
            return carts.ofCustomer(customer.get());
        }
        return CartCookie.read(request).filter(id -> carts.belongsTo(id, null));
    }

    /** After a change: guests get the cookie for a new bag. */
    public void remember(HttpServletRequest request, HttpServletResponse response, UUID cartId) {
        if (CurrentCustomer.id().isEmpty() && CartCookie.read(request).filter(cartId::equals).isEmpty()) {
            CartCookie.write(request, response, cartId);
        }
    }

    /** At sign-in: moves the guest bag into the customer's bag and drops the cookie. */
    public void claim(long customerId, HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> guest = CartCookie.read(request);
        carts.claim(customerId, guest);
        if (guest.isPresent()) {
            CartCookie.clear(request, response);
        }
    }

    public void forget(HttpServletRequest request, HttpServletResponse response) {
        if (CartCookie.read(request).isPresent()) {
            CartCookie.clear(request, response);
        }
    }
}
