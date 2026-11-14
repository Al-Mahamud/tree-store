package com.shobuj.store.support;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * A stand-in for SSLCommerz in tests and local runs: its session, validation and transaction query
 * APIs, plus a small payment page for a browser. Paying there sends the IPN the way the gateway
 * would, then sends the browser back. Exists only in test sources, never in the real application.
 */
@Profile({"test", "local"})
@RestController
@RequestMapping("/api/v1/test-gateway")
public class FakeSslCommerz {

    private final Map<String, Map<String, String>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();
    private volatile String refuseNextSession;
    /** Tests can hold the IPN back, as when it is lost on the way. */
    private volatile boolean sendIpn = true;

    public void refuseNextSession(String reason) {
        refuseNextSession = reason;
    }

    public void sendIpn(boolean send) {
        sendIpn = send;
    }

    public Map<String, String> session(String tranId) {
        return sessions.get(tranId);
    }

    /** Records a payment as the gateway would and returns its val_id. */
    public String pay(String tranId, String amount, String currency, String cardType) {
        String valId = "VAL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("status", "VALID");
        payment.put("tran_id", tranId);
        payment.put("val_id", valId);
        payment.put("amount", amount);
        payment.put("currency", currency);
        payment.put("card_type", cardType);
        payment.put("bank_tran_id", "BANK" + valId.substring(3, 11));
        payments.put(valId, payment);
        return valId;
    }

    @PostMapping(path = "/gwprocess/v4/api.php", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> createSession(@RequestBody MultiValueMap<String, String> form) {
        Map<String, String> fields = new LinkedHashMap<>();
        form.forEach((key, values) -> fields.put(key, values.get(0)));
        String reason = refuseNextSession;
        if (reason != null) {
            refuseNextSession = null;
            return Map.of("status", "FAILED", "failedreason", reason);
        }
        sessions.put(fields.get("tran_id"), fields);
        return Map.of("status", "SUCCESS", "sessionkey", "S" + fields.get("tran_id"),
                "GatewayPageURL", ServletUriComponentsBuilder.fromCurrentContextPath().toUriString()
                        + "/api/v1/test-gateway/checkout/" + fields.get("tran_id"));
    }

    /** The payment page a shopper sees in a local run. */
    @GetMapping("/checkout/{tranId}")
    ResponseEntity<String> page(@PathVariable String tranId) {
        Map<String, String> s = sessions.get(tranId);
        if (s == null) {
            return ResponseEntity.notFound().build();
        }
        String base = "/api/v1/test-gateway/checkout/" + tranId;
        String page = """
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Test payment</title>
                <meta name="viewport" content="width=device-width, initial-scale=1"><style>
                body{font:16px system-ui,sans-serif;background:#eef1f5;color:#1d2433;margin:0;display:grid;place-items:center;min-height:100vh}
                main{background:#fff;border-radius:18px;padding:28px;width:min(380px,calc(100vw - 32px));box-shadow:0 20px 50px -25px #0004}
                .tag{display:inline-block;background:#fff3c4;border-radius:8px;padding:4px 10px;font-size:12px;font-weight:700}
                h1{font-size:20px;margin:14px 0 2px} .amount{font-size:34px;font-weight:800;margin:6px 0 18px}
                .muted{color:#667085;font-size:14px} a{display:block;text-align:center;text-decoration:none;font-weight:700;border-radius:12px;padding:13px;margin-top:10px}
                .pay{color:#fff} .bkash{background:#e2136e} .nagad{background:#f6921e} .card{background:#1d2433}
                .other{background:#eef1f5;color:#1d2433}
                </style></head><body><main>
                <span class="tag">TEST GATEWAY · NOT REAL MONEY</span>
                <h1>%s</h1><p class="muted">%s · %s</p><p class="amount">৳%s</p>
                <a class="pay bkash" href="%s/pay?method=bKash">Pay with bKash</a>
                <a class="pay nagad" href="%s/pay?method=Nagad">Pay with Nagad</a>
                <a class="pay card" href="%s/pay?method=VISA">Pay by card</a>
                <a class="other" href="%s/fail">Make the payment fail</a>
                <a class="other" href="%s/cancel">Cancel and go back</a>
                </main></body></html>
                """.formatted(escape(s.get("product_name")), escape(s.get("cus_name")), escape(s.get("cus_phone")),
                escape(s.get("total_amount")), base, base, base, base, base);
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(page);
    }

    @GetMapping("/checkout/{tranId}/{choice}")
    ResponseEntity<Void> choose(@PathVariable String tranId, @PathVariable String choice,
                                @RequestParam(defaultValue = "bKash") String method) {
        Map<String, String> s = sessions.get(tranId);
        if (s == null) {
            return ResponseEntity.notFound().build();
        }
        LinkedMultiValueMap<String, String> ipn = new LinkedMultiValueMap<>();
        ipn.add("tran_id", tranId);
        String back;
        switch (choice) {
            case "pay" -> {
                String valId = pay(tranId, s.get("total_amount"), "BDT", method);
                ipn.add("status", "VALID");
                ipn.add("val_id", valId);
                back = s.get("success_url") + "?tran_id=" + tranId + "&val_id=" + valId + "&status=VALID";
            }
            case "cancel" -> {
                ipn.add("status", "CANCELLED");
                back = s.get("cancel_url") + "?tran_id=" + tranId + "&status=CANCELLED";
            }
            default -> {
                ipn.add("status", "FAILED");
                back = s.get("fail_url") + "?tran_id=" + tranId + "&status=FAILED";
            }
        }
        if (sendIpn) {
            try {
                // Plain HTTP/1.1: the Next dev server, which forwards the IPN in local runs, drops HTTP/2 upgrades.
                RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build()
                        .post().uri(s.get("ipn_url")).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(ipn).retrieve().toBodilessEntity();
            } catch (RuntimeException e) {
                // Like the real gateway: a lost IPN doesn't stop the shopper going back.
                LoggerFactory.getLogger(FakeSslCommerz.class).warn("Fake gateway: IPN to {} failed", s.get("ipn_url"), e);
            }
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, back).build();
    }

    @GetMapping("/validator/api/validationserverAPI.php")
    Map<String, Object> validate(@RequestParam("val_id") String valId, @RequestParam("store_id") String storeId) {
        Map<String, Object> payment = payments.get(valId);
        if (payment == null || !"test-store".equals(storeId)) {
            return Map.of("status", "INVALID_TRANSACTION");
        }
        Map<String, Object> answer = new LinkedHashMap<>(payment);
        answer.put("status", Boolean.TRUE.equals(payment.get("validated")) ? "VALIDATED" : payment.get("status"));
        answer.remove("validated");
        payment.put("validated", true);
        return answer;
    }

    @GetMapping("/validator/api/merchantTransIDvalidationAPI.php")
    Map<String, Object> query(@RequestParam("tran_id") String tranId) {
        List<Map<String, Object>> found = payments.values().stream().filter(p -> tranId.equals(p.get("tran_id")))
                .map(p -> {
                    Map<String, Object> copy = new LinkedHashMap<>(p);
                    copy.remove("validated");
                    return copy;
                }).toList();
        return Map.of("APIConnect", "DONE", "no_of_trans_found", found.size(), "element", found);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
