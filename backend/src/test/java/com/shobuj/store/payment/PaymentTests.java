package com.shobuj.store.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.ApiClient.Response;
import com.shobuj.store.support.FakeSslCommerz;
import com.shobuj.store.support.IntegrationTest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class PaymentTests {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    FakeSslCommerz gateway;

    @Autowired
    PaymentService payments;

    final HttpClient browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    ApiClient shopper;
    long plant;

    record Placed(String number, String token, String paymentUrl) {
        String tranId(int attempt) {
            return number + "-" + attempt;
        }
    }

    @BeforeEach
    void setUp() {
        shopper = new ApiClient(port);
        plant = jdbc.sql("""
                        select v.id from variants v join products p on p.id = v.product_id
                         where p.slug = 'snake-plant' and v.size = 'MEDIUM' order by v.price limit 1""")
                .query(Long.class).single();
        jdbc.sql("update variants set stock = 80 where id = :id").param("id", plant).update();
    }

    @AfterEach
    void tearDown() {
        gateway.sendIpn(true);
    }

    Placed placeOnline() {
        shopper.post("/api/v1/cart/items", Map.of("variantId", plant, "quantity", 1));
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Rafiq Islam");
        body.put("phone", "01811223344");
        body.put("address", "House 3, Road 11");
        body.put("area", "Banani");
        body.put("city", "Dhaka");
        body.put("zone", "DHAKA");
        body.put("paymentMethod", "ONLINE");
        Response r = shopper.post("/api/v1/checkout", body);
        assertThat(r.status()).isEqualTo(201);
        JsonNode url = r.body().get("paymentUrl");
        return new Placed(r.body().get("number").asString(), r.body().get("token").asString(),
                url == null || url.isNull() ? null : url.asString());
    }

    JsonNode order(Placed p) {
        return shopper.get("/api/v1/orders/" + p.number() + "?token=" + URLEncoder.encode(p.token(), StandardCharsets.UTF_8)).body();
    }

    HttpResponse<String> open(String url) throws Exception {
        return browser.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    int ipn(Map<String, String> fields) throws Exception {
        String form = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return browser.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/payments/sslcommerz/ipn"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    int stock() {
        return jdbc.sql("select stock from variants where id = :id").param("id", plant).query(Integer.class).single();
    }

    @Test
    void anOnlineOrderWaitsForPaymentThenThePaymentConfirmsIt() throws Exception {
        assertThat(shopper.get("/api/v1/payments/options").body().get("online").asBoolean()).isTrue();
        Placed placed = placeOnline();
        assertThat(placed.paymentUrl()).contains("/api/v1/test-gateway/checkout/" + placed.tranId(1));
        JsonNode waiting = order(placed);
        assertThat(waiting.get("status").asString()).isEqualTo("PENDING_PAYMENT");
        assertThat(waiting.get("paymentExpiresAt").isNull()).isFalse();
        assertThat(stock()).isEqualTo(79);
        Map<String, String> session = gateway.session(placed.tranId(1));
        assertThat(session.get("total_amount")).isEqualTo(waiting.get("total").asInt() + ".00");
        assertThat(session.get("ipn_url")).endsWith("/api/v1/payments/sslcommerz/ipn");

        HttpResponse<String> page = open(placed.paymentUrl());
        assertThat(page.body()).contains("TEST GATEWAY");
        HttpResponse<String> paid = open(placed.paymentUrl() + "/pay?method=bKash");
        assertThat(paid.statusCode()).isEqualTo(303);

        JsonNode done = order(placed);
        assertThat(done.get("status").asString()).isEqualTo("PLACED");
        assertThat(done.get("paymentStatus").asString()).isEqualTo("PAID");
        assertThat(done.get("paymentExpiresAt").isNull()).isTrue();
        assertThat(done.get("events").toString()).contains("bKash");

        // The browser's own return then lands on the order page; a second confirmation changes nothing.
        HttpResponse<String> back = open(paid.headers().firstValue("Location").orElseThrow());
        assertThat(back.statusCode()).isEqualTo(303);
        assertThat(back.headers().firstValue("Location").orElseThrow())
                .startsWith("http://shop.test/order/" + placed.number() + "?t=").endsWith("&payment=success");
        assertThat(StreamSupport.stream(order(placed).get("events").spliterator(), false)
                .filter(e -> e.get("status").asString().equals("PLACED")).count()).isEqualTo(1);
        assertThat(stock()).isEqualTo(79);
    }

    @Test
    void theReturnAloneConfirmsWhenTheIpnIsLost() throws Exception {
        gateway.sendIpn(false);
        Placed placed = placeOnline();
        HttpResponse<String> paid = open(placed.paymentUrl() + "/pay?method=Nagad");
        assertThat(order(placed).get("status").asString()).isEqualTo("PENDING_PAYMENT");
        open(paid.headers().firstValue("Location").orElseThrow());
        assertThat(order(placed).get("paymentStatus").asString()).isEqualTo("PAID");
    }

    @Test
    void theBackgroundCheckFindsPaymentsNobodyReported() {
        Placed placed = placeOnline();
        gateway.pay(placed.tranId(1), gateway.session(placed.tranId(1)).get("total_amount"), "BDT", "VISA");
        jdbc.sql("update payment_attempts set created_at = now() - interval '5 minutes' where tran_id = :t")
                .param("t", placed.tranId(1)).update();
        payments.checkPending();
        assertThat(order(placed).get("status").asString()).isEqualTo("PLACED");
    }

    @Test
    void forgedOrMismatchedCallbacksChangeNothing() throws Exception {
        Placed placed = placeOnline();
        String tran = placed.tranId(1);
        assertThat(ipn(Map.of("tran_id", tran, "status", "VALID", "val_id", "MADEUP123"))).isEqualTo(200);
        // A real payment, but for the wrong amount.
        String cheap = gateway.pay(tran, "1.00", "BDT", "bKash");
        ipn(Map.of("tran_id", tran, "status", "VALID", "val_id", cheap));
        // A real payment for a different order's tran_id can't be borrowed.
        Placed other = placeOnline();
        String otherVal = gateway.pay(other.tranId(1), gateway.session(other.tranId(1)).get("total_amount"), "BDT", "bKash");
        ipn(Map.of("tran_id", tran, "status", "VALID", "val_id", otherVal));
        open("http://localhost:" + port + "/api/v1/payments/sslcommerz/return/success?tran_id=" + tran + "&val_id=" + otherVal);
        assertThat(order(placed).get("status").asString()).isEqualTo("PENDING_PAYMENT");
        assertThat(order(placed).get("paymentStatus").asString()).isEqualTo("UNPAID");
        assertThat(ipn(Map.of("tran_id", "NOPE-1", "status", "VALID"))).isEqualTo(200);
        int stored = jdbc.sql("select count(*) from gateway_callbacks where tran_id = :t").param("t", tran).query(Integer.class).single();
        assertThat(stored).isGreaterThanOrEqualTo(4);
    }

    @Test
    void aFailedPaymentCanBeRetriedOrSwitchedToCash() throws Exception {
        Placed placed = placeOnline();
        HttpResponse<String> failed = open(placed.paymentUrl() + "/fail");
        assertThat(failed.headers().firstValue("Location").orElseThrow()).contains("/return/fail");
        HttpResponse<String> back = open(failed.headers().firstValue("Location").orElseThrow());
        assertThat(back.headers().firstValue("Location").orElseThrow()).endsWith("&payment=fail");
        assertThat(order(placed).get("status").asString()).isEqualTo("PENDING_PAYMENT");
        assertThat(jdbc.sql("select status from payment_attempts where tran_id = :t").param("t", placed.tranId(1))
                .query(String.class).single()).isEqualTo("FAILED");

        Response stranger = new ApiClient(port).post("/api/v1/orders/" + placed.number() + "/pay", Map.of("token", "guess"));
        assertThat(stranger.status()).isEqualTo(404);
        Response retry = shopper.post("/api/v1/orders/" + placed.number() + "/pay", Map.of("token", placed.token()));
        assertThat(retry.body().get("paymentUrl").asString()).endsWith(placed.tranId(2));

        JsonNode cash = shopper.post("/api/v1/orders/" + placed.number() + "/cash-on-delivery", Map.of("token", placed.token())).body();
        assertThat(cash.get("status").asString()).isEqualTo("PLACED");
        assertThat(cash.get("paymentMethod").asString()).isEqualTo("COD");
        Response again = shopper.post("/api/v1/orders/" + placed.number() + "/pay", Map.of("token", placed.token()));
        assertThat(again.status()).isEqualTo(409);
    }

    @Test
    void unpaidOrdersExpireAndGiveEverythingBack() throws Exception {
        Placed placed = placeOnline();
        assertThat(stock()).isEqualTo(79);
        jdbc.sql("update orders set payment_expires_at = now() - interval '1 minute' where number = :n")
                .param("n", placed.number()).update();
        Response tooLate = shopper.post("/api/v1/orders/" + placed.number() + "/pay", Map.of("token", placed.token()));
        assertThat(tooLate.body().get("code").asString()).isEqualTo("payment.expired");

        payments.checkPending();
        JsonNode cancelled = order(placed);
        assertThat(cancelled.get("status").asString()).isEqualTo("CANCELLED");
        assertThat(stock()).isEqualTo(80);
        assertThat(jdbc.sql("select count(*) from payment_attempts where tran_id = :t and status = 'CANCELLED'")
                .param("t", placed.tranId(1)).query(Integer.class).single()).isEqualTo(1);

        // Money that arrives after that is recorded for a refund; the order stays cancelled.
        String late = gateway.pay(placed.tranId(1), gateway.session(placed.tranId(1)).get("total_amount"), "BDT", "bKash");
        ipn(Map.of("tran_id", placed.tranId(1), "status", "VALID", "val_id", late));
        JsonNode after = order(placed);
        assertThat(after.get("status").asString()).isEqualTo("CANCELLED");
        assertThat(after.get("paymentStatus").asString()).isEqualTo("PAID");
        assertThat(after.get("events").toString()).contains("Refund");
        assertThat(stock()).isEqualTo(80);
    }

    @Test
    void whenTheGatewayRefusesTheOrderStillWaitsForPayment() {
        gateway.refuseNextSession("Store is not active");
        Placed placed = placeOnline();
        assertThat(placed.paymentUrl()).isNull();
        assertThat(order(placed).get("status").asString()).isEqualTo("PENDING_PAYMENT");
        Response retry = shopper.post("/api/v1/orders/" + placed.number() + "/pay", Map.of("token", placed.token()));
        assertThat(retry.status()).isEqualTo(200);
    }
}
