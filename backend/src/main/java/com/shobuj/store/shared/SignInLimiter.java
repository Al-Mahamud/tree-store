package com.shobuj.store.shared;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing: after too many wrong passwords for one number, or from one
 * address, sign-in pauses for a while. Kept in memory; one server is enough for v1.
 */
@Component
public class SignInLimiter {

    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final int PER_PHONE = 6;
    public static final int PER_ADDRESS = 30;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    SignInLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String phone, String address) {
        if (count("p:" + phone) >= PER_PHONE || count("a:" + address) >= PER_ADDRESS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "auth.too_many",
                    "Too many wrong passwords. Please wait 15 minutes and try again.");
        }
    }

    public void failed(String phone, String address) {
        Instant now = clock.instant();
        for (String key : new String[] {"p:" + phone, "a:" + address}) {
            failures.compute(key, (k, q) -> {
                Deque<Instant> queue = q == null ? new ArrayDeque<>() : q;
                queue.addLast(now);
                return queue;
            });
        }
    }

    public void succeeded(String phone) {
        failures.remove("p:" + phone);
    }

    private int count(String key) {
        Instant cutoff = clock.instant().minus(WINDOW);
        Deque<Instant> queue = failures.computeIfPresent(key, (k, q) -> {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
                q.removeFirst();
            }
            return q.isEmpty() ? null : q;
        });
        return queue == null ? 0 : queue.size();
    }
}
