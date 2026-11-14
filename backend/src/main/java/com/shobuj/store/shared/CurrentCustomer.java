package com.shobuj.store.shared;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The signed-in customer of the current request, if any. A customer's session stores the
 * principal name {@code customer:<id>}.
 */
public final class CurrentCustomer {

    public static final String PREFIX = "customer:";

    private CurrentCustomer() {
    }

    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || !auth.getName().startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(auth.getName().substring(PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static long require() {
        return id().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth.required", "Please sign in."));
    }
}
