package com.shobuj.store.checkout;

import com.shobuj.store.checkout.CartService.CartView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The shopper's bag. Every change answers with the whole bag, so the page can redraw from one response. */
@RestController
@RequestMapping("/api/v1/cart")
class CartController {

    record AddItem(@NotNull Long variantId, @Min(1) @Max(10) int quantity) {
    }

    record ChangeQuantity(@Min(0) @Max(10) int quantity) {
    }

    record ApplyCoupon(@NotBlank String code) {
    }

    private final CartService carts;

    CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    CartView get(HttpServletRequest request) {
        return carts.view(CartCookie.read(request));
    }

    @PostMapping("/items")
    CartView add(@Valid @RequestBody AddItem body, HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> current = CartCookie.read(request);
        UUID id = carts.add(current, body.variantId(), body.quantity() == 0 ? 1 : body.quantity());
        if (current.filter(id::equals).isEmpty()) {
            CartCookie.write(request, response, id);
        }
        return carts.view(Optional.of(id));
    }

    @PatchMapping("/items/{variantId}")
    CartView change(@PathVariable long variantId, @Valid @RequestBody ChangeQuantity body, HttpServletRequest request) {
        UUID id = CartCookie.read(request).orElseThrow(CartService::emptyCart);
        carts.setQuantity(id, variantId, body.quantity());
        return carts.view(Optional.of(id));
    }

    @DeleteMapping("/items/{variantId}")
    CartView remove(@PathVariable long variantId, HttpServletRequest request) {
        Optional<UUID> id = CartCookie.read(request);
        id.ifPresent(cart -> carts.remove(cart, variantId));
        return carts.view(id);
    }

    @PostMapping("/coupon")
    CartView applyCoupon(@Valid @RequestBody ApplyCoupon body, HttpServletRequest request) {
        UUID id = CartCookie.read(request).orElseThrow(CartService::emptyCart);
        carts.applyCoupon(id, body.code());
        return carts.view(Optional.of(id));
    }

    @DeleteMapping("/coupon")
    CartView removeCoupon(HttpServletRequest request) {
        Optional<UUID> id = CartCookie.read(request);
        id.ifPresent(carts::removeCoupon);
        return carts.view(id);
    }
}
