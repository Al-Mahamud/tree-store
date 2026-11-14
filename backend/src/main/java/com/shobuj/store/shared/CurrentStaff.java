package com.shobuj.store.shared;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The signed-in member of staff of the current request (principal {@code staff:<id>}). */
public final class CurrentStaff {

    public static final String PREFIX = "staff:";

    private CurrentStaff() {
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
