package com.shobuj.store.payment;

import com.shobuj.store.shared.StoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * What SSLCommerz sends: the IPN (server to server) and the shopper's browser coming back. Neither is
 * signed in or carries a CSRF token, so nothing in them is believed: each is stored as received, and
 * a payment counts only once the gateway's Validation API confirms its val_id for our tran_id and amount.
 */
@RestController
@RequestMapping("/api/v1/payments/sslcommerz")
class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private record OrderRef(String number, String token) {
    }

    private final JdbcClient jdbc;
    private final SslCommerzClient gateway;
    private final PaymentService payments;
    private final String webUrl;
    private final JsonMapper json = JsonMapper.builder().build();

    GatewayController(JdbcClient jdbc, SslCommerzClient gateway, PaymentService payments, StoreProperties properties) {
        this.jdbc = jdbc;
        this.gateway = gateway;
        this.payments = payments;
        this.webUrl = properties.webUrl();
    }

    /** 200 once handled (unknown tran_ids too, so the gateway stops retrying); 500 makes it try again. */
    @PostMapping("/ipn")
    ResponseEntity<String> ipn(HttpServletRequest request) {
        Map<String, String> fields = fields(request);
        String tranId = fields.getOrDefault("tran_id", "");
        store("IPN", tranId, fields);
        if (order(tranId).isEmpty()) {
            log.warn("IPN for unknown tran_id {}", tranId);
            return ResponseEntity.ok("UNKNOWN");
        }
        String status = fields.getOrDefault("status", "");
        if (!status.equals("VALID") && !status.equals("VALIDATED")) {
            payments.failed(tranId, status);
            return ResponseEntity.ok("NOTED");
        }
        payments.confirm(tranId, gateway.validate(fields.getOrDefault("val_id", "")));
        return ResponseEntity.ok("OK");
    }

    /**
     * The browser back from the payment page. On success its val_id is checked with the gateway at
     * once, so the shopper needn't wait for the IPN; then the browser goes to the order page.
     */
    @RequestMapping(path = "/return/{result}", method = {RequestMethod.GET, RequestMethod.POST})
    ResponseEntity<Void> back(@PathVariable String result, HttpServletRequest request) {
        String outcome = switch (result) {
            case "success", "cancel" -> result;
            default -> "fail";
        };
        Map<String, String> fields = fields(request);
        String tranId = fields.getOrDefault("tran_id", "");
        store("RETURN_" + outcome.toUpperCase(), tranId, fields);
        Optional<OrderRef> order = order(tranId);
        if (order.isEmpty()) {
            return redirect(webUrl + "/track");
        }
        if (outcome.equals("success") && !fields.getOrDefault("val_id", "").isBlank()) {
            try {
                payments.confirm(tranId, gateway.validate(fields.get("val_id")));
            } catch (RuntimeException e) {
                // The IPN or the background check will apply it.
                log.warn("Checking payment {} on the browser's return failed", tranId, e);
            }
        } else if (!outcome.equals("success")) {
            payments.failed(tranId, outcome.equals("cancel") ? "CANCELLED" : "FAILED");
        }
        return redirect(webUrl + "/order/" + order.get().number() + "?t="
                + URLEncoder.encode(order.get().token(), StandardCharsets.UTF_8) + "&payment=" + outcome);
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(url)).build();
    }

    private Optional<OrderRef> order(String tranId) {
        if (tranId.isBlank()) {
            return Optional.empty();
        }
        return jdbc.sql("""
                        select o.number, o.access_token from payment_attempts a join orders o on o.id = a.order_id
                         where a.tran_id = :t""")
                .param("t", tranId).query((rs, n) -> new OrderRef(rs.getString(1), rs.getString(2))).optional();
    }

    private void store(String kind, String tranId, Map<String, String> fields) {
        Map<String, String> safe = new TreeMap<>(fields);
        safe.remove("store_passwd");
        jdbc.sql("insert into gateway_callbacks (kind, tran_id, body) values (:k, :t, :b)")
                .param("k", kind).param("t", tranId.isBlank() ? null : tranId).param("b", json.writeValueAsString(safe))
                .update();
    }

    private static Map<String, String> fields(HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();
        request.getParameterMap().forEach((name, values) -> fields.put(name, values.length == 0 ? "" : values[0]));
        return fields;
    }
}
