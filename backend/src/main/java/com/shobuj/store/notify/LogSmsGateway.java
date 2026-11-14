package com.shobuj.store.notify;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local runs: texts go to the log (and a short in-memory list) instead of a phone. */
public class LogSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LogSmsGateway.class);

    public record Sent(String phone, String text, Instant at) {
    }

    private final Deque<Sent> recent = new ArrayDeque<>();

    @Override
    public String name() {
        return "log";
    }

    @Override
    public synchronized Result send(String phone, String text, String reference) {
        log.info("[SMS to {}] {}", phone, text);
        recent.addFirst(new Sent(phone, text, Instant.now()));
        while (recent.size() > 50) {
            recent.removeLast();
        }
        return Result.ok("log-" + reference);
    }

    /** The latest texts, newest first. */
    public synchronized List<Sent> recent() {
        return List.copyOf(recent);
    }
}
