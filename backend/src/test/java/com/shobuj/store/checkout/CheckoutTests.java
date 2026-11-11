package com.shobuj.store.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.ApiClient.Response;
import com.shobuj.store.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class CheckoutTests {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    ApiClient shopper;

    @BeforeEach
    void setUp() {
        shopper = new ApiClient(port);
    }

    /** An option of the plant in the given size with at least {@code minStock} in stock. */
    long variant(String slug, String size, int minStock) {
        return jdbc.sql("""
                        select v.id from variants v join products p on p.id = v.product_id
                         where p.slug = :slug and v.size = :size and v.stock >= :min
                         order by v.price, v.id limit 1""")
                .param("slug", slug).param("size", size).param("min", minStock).query(Long.class).single();
    }

    int price(long variantId) {
        return jdbc.sql("select price from variants where id = :id").param("id", variantId).query(Integer.class).single();
    }

    int stock(long variantId) {
        return jdbc.sql("select stock from variants where id = :id").param("id", variantId).query(Integer.class).single();
    }

    Map<String, Object> address(String zone) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Rafiq Islam");
        body.put("phone", "01812-345678");
        body.put("address", "Flat 4B, House 9, Road 11");
        body.put("area", "Banani");
        body.put("city", zone.equals("DHAKA") ? "Dhaka" : "Chattogram");
        body.put("zone", zone);
        body.put("paymentMethod", "COD");
        return body;
    }

    @Test
    void bagUsesTheOptionPriceAndPlanter() {
        long mango = variant("amrapali-mango", "SMALL", 2);
        JsonNode cart = shopper.post("/api/v1/cart/items", Map.of("variantId", mango, "quantity", 2)).body();
        JsonNode line = cart.get("items").get(0);
        assertThat(line.get("size").asString()).isEqualTo("Small");
        assertThat(line.get("height").asString()).isEqualTo("1–2 ft");
        assertThat(line.get("planter").asString()).isIn("Grow bag", "Clay pot");
        assertThat(cart.get("subtotal").asInt()).isEqualTo(2 * price(mango));
        assertThat(cart.get("remainingForFreeDelivery").asInt()).isEqualTo(5000 - 2 * price(mango));
        assertThat(cart.get("dhakaOnly").asBoolean()).isFalse();
    }

    @Test
    void largeTreesAreDeliveredInsideDhakaOnly() {
        long big = variant("krishnachura", "LARGE", 1);
        JsonNode cart = shopper.post("/api/v1/cart/items", Map.of("variantId", big, "quantity", 1)).body();
        assertThat(cart.get("dhakaOnly").asBoolean()).isTrue();

        JsonNode outside = shopper.post("/api/v1/checkout/quote", Map.of("zone", "OUTSIDE")).body();
        assertThat(outside.get("problem").asString()).contains("Krishnachura").contains("inside Dhaka only");
        Response refused = shopper.post("/api/v1/checkout", address("OUTSIDE"));
        assertThat(refused.status()).isEqualTo(409);
        assertThat(refused.body().get("code").asString()).isEqualTo("checkout.dhaka_only");

        JsonNode inside = shopper.post("/api/v1/checkout/quote", Map.of("zone", "DHAKA")).body();
        assertThat(inside.get("problem").isNull()).isTrue();
        assertThat(shopper.post("/api/v1/checkout", address("DHAKA")).status()).isEqualTo(201);
    }

    @Test
    void placingACashOnDeliveryOrder() {
        long snake = variant("snake-plant", "SMALL", 1);
        int before = stock(snake);
        int price = price(snake);
        shopper.post("/api/v1/cart/items", Map.of("variantId", snake, "quantity", 1));

        JsonNode quote = shopper.post("/api/v1/checkout/quote", Map.of("zone", "OUTSIDE")).body();
        assertThat(quote.get("shippingFee").asInt()).isEqualTo(200);
        assertThat(quote.get("total").asInt()).isEqualTo(price + 200);

        Response placed = shopper.post("/api/v1/checkout", address("OUTSIDE"));
        assertThat(placed.status()).isEqualTo(201);
        String number = placed.body().get("number").asString();
        String token = placed.body().get("token").asString();
        assertThat(number).startsWith("SB");
        assertThat(stock(snake)).isEqualTo(before - 1);
        assertThat(shopper.get("/api/v1/cart").body().get("items")).isEmpty();

        JsonNode order = shopper.get("/api/v1/orders/" + number + "?token=" + token).body();
        assertThat(order.get("status").asString()).isEqualTo("PLACED");
        assertThat(order.get("phone").asString()).isEqualTo("01812345678");
        JsonNode item = order.get("items").get(0);
        assertThat(item.get("name").asString()).isEqualTo("Snake Plant");
        assertThat(item.get("height").asString()).isEqualTo("30–45 cm");
        assertThat(order.get("total").asInt()).isEqualTo(price + 200);

        assertThat(shopper.get("/api/v1/orders/" + number + "?token=nope").status()).isEqualTo(404);
        ApiClient stranger = new ApiClient(port);
        assertThat(stranger.get("/api/v1/orders/track?number=" + number + "&phone=01812345678").status()).isEqualTo(200);
        assertThat(stranger.get("/api/v1/orders/track?number=" + number + "&phone=01712345678").status()).isEqualTo(404);
    }

    @Test
    void deliveryIsFreeOverTheThreshold() {
        long bonsai = variant("ficus-bonsai", "MEDIUM", 1);
        shopper.post("/api/v1/cart/items", Map.of("variantId", bonsai, "quantity", 2));
        JsonNode quote = shopper.post("/api/v1/checkout/quote", Map.of("zone", "OUTSIDE")).body();
        assertThat(quote.get("shippingFee").asInt()).isZero();
    }

    @Test
    void couponsCheckTheirRules() {
        long neem = variant("neem", "SMALL", 1);
        shopper.post("/api/v1/cart/items", Map.of("variantId", neem, "quantity", 1));
        Response tooSmall = shopper.post("/api/v1/cart/coupon", Map.of("code", "green10"));
        assertThat(tooSmall.status()).isEqualTo(422);
        assertThat(tooSmall.body().get("code").asString()).isEqualTo("coupon.min_subtotal");
        assertThat(shopper.post("/api/v1/cart/coupon", Map.of("code", "SPRING25")).body().get("code").asString())
                .isEqualTo("coupon.expired");

        long monstera = variant("monstera-deliciosa", "MEDIUM", 1);
        shopper.post("/api/v1/cart/items", Map.of("variantId", monstera, "quantity", 1));
        JsonNode cart = shopper.post("/api/v1/cart/coupon", Map.of("code", "GREEN10")).body();
        int subtotal = price(neem) + price(monstera);
        assertThat(cart.get("discount").asInt()).isEqualTo(Math.min(500, subtotal / 10));
    }

    @Test
    void checkoutValidatesTheAddress() {
        shopper.post("/api/v1/cart/items", Map.of("variantId", variant("money-plant", "SMALL", 1), "quantity", 1));
        Map<String, Object> body = address("DHAKA");
        body.put("phone", "999");
        Response bad = shopper.post("/api/v1/checkout", body);
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.body().get("fields").get("phone").asString()).contains("11-digit");
    }

    @Test
    void emptyBagAndCsrf() {
        assertThat(shopper.post("/api/v1/checkout", address("DHAKA")).body().get("code").asString()).isEqualTo("cart.empty");
        assertThat(new ApiClient(port).postWithoutCsrf("/api/v1/cart/items",
                Map.of("variantId", variant("neem", "SMALL", 1), "quantity", 1)).status()).isEqualTo(403);
    }
}
