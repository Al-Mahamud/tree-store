package com.shobuj.store.checkout;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/** The cookie that remembers which bag belongs to this browser. */
final class CartCookie {

    static final String NAME = "shobuj_cart";

    private CartCookie() {
    }

    static Optional<UUID> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(NAME))
                .findFirst()
                .flatMap(c -> {
                    try {
                        return Optional.of(UUID.fromString(c.getValue()));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                });
    }

    static void write(HttpServletRequest request, HttpServletResponse response, UUID cartId) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, cartId.toString(), Duration.ofDays(60)).toString());
    }

    static void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, "", Duration.ZERO).toString());
    }

    private static ResponseCookie cookie(HttpServletRequest request, String value, Duration maxAge) {
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Lax").path("/")
                .maxAge(maxAge).build();
    }
}
