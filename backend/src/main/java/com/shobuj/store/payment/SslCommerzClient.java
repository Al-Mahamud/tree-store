package com.shobuj.store.payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * SSLCommerz's APIs: open a hosted payment page, validate a payment by its {@code val_id}, and
 * look up every payment for one of our {@code tran_id}s. Settings ({@code store.payment.sslcommerz.*})
 * are read on each call, so tests can point them at a fake gateway on the app's own port.
 */
@Component
public class SslCommerzClient {

    /** What the gateway says about a payment, with the fields we check. */
    public record Payment(String status, String tranId, String valId, BigDecimal amount, String currency,
                          String cardType, String bankTranId) {

        /** VALID the first time it is checked, VALIDATED after that: either way, paid. */
        public boolean paid() {
            return "VALID".equals(status) || "VALIDATED".equals(status);
        }
    }

    /** {@code gatewayUrl} is null when the gateway refused, with its reason. */
    public record Session(String gatewayUrl, String failedReason) {
    }

    private final Environment environment;
    private final JsonMapper json = JsonMapper.builder().build();
    private final RestClient http;

    SslCommerzClient(Environment environment) {
        this.environment = environment;
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(5));
        requests.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(requests).build();
    }

    /** Online payment is offered only once the store's credentials are set. */
    public boolean configured() {
        return !storeId().isBlank() && !password().isBlank();
    }

    /** Where the gateway sends the browser back and posts its IPN. */
    public String callbackUrl() {
        return environment.getProperty("store.payment.sslcommerz.callback-url", "http://localhost:3000");
    }

    public Session session(Map<String, String> fields) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", storeId());
        form.add("store_passwd", password());
        fields.forEach(form::add);
        String body = http.post().uri(baseUrl() + "/gwprocess/v4/api.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        JsonNode answer = json.readTree(body);
        String url = answer.path("GatewayPageURL").asString("");
        if ("SUCCESS".equalsIgnoreCase(answer.path("status").asString("")) && !url.isBlank()) {
            return new Session(url, null);
        }
        return new Session(null, answer.path("failedreason").asString("unknown"));
    }

    /** The Validation API. A callback is never believed without it. */
    public Payment validate(String valId) {
        String body = http.get().uri(baseUrl() + "/validator/api/validationserverAPI.php?val_id={val}&store_id={id}"
                        + "&store_passwd={password}&v=1&format=json", valId, storeId(), password())
                .retrieve()
                .body(String.class);
        return payment(json.readTree(body));
    }

    /** The transaction query API: every payment the gateway holds for our tran_id. */
    public List<Payment> query(String tranId) {
        String body = http.get().uri(baseUrl() + "/validator/api/merchantTransIDvalidationAPI.php?tran_id={tran}"
                        + "&store_id={id}&store_passwd={password}&format=json", tranId, storeId(), password())
                .retrieve()
                .body(String.class);
        return json.readTree(body).path("element").valueStream().map(SslCommerzClient::payment).toList();
    }

    private static Payment payment(JsonNode node) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(node.path("amount").asString("0"));
        } catch (NumberFormatException e) {
            amount = BigDecimal.ZERO;
        }
        return new Payment(node.path("status").asString(""), node.path("tran_id").asString(""),
                node.path("val_id").asString(""), amount, node.path("currency").asString(""),
                node.path("card_type").asString(""), node.path("bank_tran_id").asString(""));
    }

    private String baseUrl() {
        return environment.getProperty("store.payment.sslcommerz.base-url", "https://sandbox.sslcommerz.com");
    }

    private String storeId() {
        return environment.getProperty("store.payment.sslcommerz.store-id", "");
    }

    private String password() {
        return environment.getProperty("store.payment.sslcommerz.store-password", "");
    }
}
