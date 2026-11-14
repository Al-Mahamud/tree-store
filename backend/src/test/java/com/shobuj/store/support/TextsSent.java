package com.shobuj.store.support;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Reads the text-message outbox in tests (the sending job is off, so codes are still readable). */
public final class TextsSent {

    private TextsSent() {
    }

    public static String lastCode(JdbcClient jdbc, String phone) {
        String body = jdbc.sql("select body from sms_messages where phone = :p and kind = 'CODE' order by id desc limit 1")
                .param("p", phone).query(String.class).single();
        Matcher m = Pattern.compile("(\\d{6})").matcher(body);
        if (!m.find()) {
            throw new IllegalStateException("No code in: " + body);
        }
        return m.group(1);
    }

    public static List<String> bodies(JdbcClient jdbc, String phone, String kind) {
        return jdbc.sql("select body from sms_messages where phone = :p and kind = :k order by id")
                .param("p", phone).param("k", kind).query(String.class).list();
    }
}
