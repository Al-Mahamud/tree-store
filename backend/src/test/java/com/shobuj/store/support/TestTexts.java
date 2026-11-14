package com.shobuj.store.support;

import com.shobuj.store.notify.LogSmsGateway;
import com.shobuj.store.notify.SmsGateway;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local runs only (test sources): the texts the shop "sent", so sign-up codes can be read without
 * a real SMS account. Open http://localhost:8080/api/v1/test-texts.
 */
@Profile({"test", "local"})
@RestController
public class TestTexts {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Asia/Dhaka"));
    private final SmsGateway gateway;

    TestTexts(SmsGateway gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/api/v1/test-texts")
    ResponseEntity<String> page() {
        String rows = gateway instanceof LogSmsGateway logged
                ? logged.recent().stream().map(m -> "<li><b>" + TIME.format(m.at()) + " · " + m.phone() + "</b><br>"
                        + m.text().replace("&", "&amp;").replace("<", "&lt;") + "</li>").collect(Collectors.joining())
                : "<li>Texts are going to a real SMS provider.</li>";
        String html = """
                <!doctype html><html lang="en"><head><meta charset="utf-8"><meta http-equiv="refresh" content="5">
                <meta name="viewport" content="width=device-width, initial-scale=1"><title>Test texts</title>
                <style>body{font:15px system-ui,sans-serif;max-width:640px;margin:24px auto;padding:0 16px;color:#1a1a2e}
                li{background:#fff7e6;border-radius:12px;padding:12px;margin:8px 0;list-style:none}ul{padding:0}</style></head>
                <body><h1>Texts sent (local run)</h1><p>Not real SMS. Newest first; refreshes every 5 seconds.</p><ul>%s</ul></body></html>
                """.formatted(rows.isEmpty() ? "<li>No texts yet.</li>" : rows);
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
    }
}
