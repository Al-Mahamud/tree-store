package com.shobuj.store.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.ApiClient.Response;
import com.shobuj.store.support.DemoData;
import com.shobuj.store.support.FakeSslCommerz;
import com.shobuj.store.support.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class AdminTests {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    FakeSslCommerz gateway;

    ApiClient admin;

    @BeforeEach
    void signIn() {
        admin = new ApiClient(port);
        assertThat(admin.post("/api/v1/staff/login",
                Map.of("phone", DemoData.STAFF_PHONE, "password", DemoData.STAFF_PASSWORD)).status()).isEqualTo(200);
    }

    /** The cheapest option of a plant in a size, with its stock set to 50. */
    long variant(String slug, String size) {
        long id = jdbc.sql("""
                        select v.id from variants v join products p on p.id = v.product_id
                         where p.slug = :s and v.size = :size order by v.price, v.id limit 1""")
                .param("s", slug).param("size", size).query(Long.class).single();
        jdbc.sql("update variants set stock = 50 where id = :id").param("id", id).update();
        return id;
    }

    int stock(long variant) {
        return jdbc.sql("select stock from variants where id = :id").param("id", variant).query(Integer.class).single();
    }

    JsonNode place(ApiClient shopper, String method, String zone) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Mitu Rahman");
        body.put("phone", "01555123456");
        body.put("address", "Flat 2C, House 8, Road 4");
        body.put("area", zone.equals("DHAKA") ? "Uttara" : "Agrabad");
        body.put("city", zone.equals("DHAKA") ? "Dhaka" : "Chattogram");
        body.put("zone", zone);
        body.put("paymentMethod", method);
        Response r = shopper.post("/api/v1/checkout", body);
        assertThat(r.status()).isEqualTo(201);
        return r.body();
    }

    @Test
    void onlyActiveStaffGetIn() {
        ApiClient anonymous = new ApiClient(port);
        assertThat(anonymous.get("/api/v1/admin/dashboard").status()).isEqualTo(401);
        ApiClient customer = new ApiClient(port);
        customer.post("/api/v1/auth/login", Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD));
        assertThat(customer.get("/api/v1/admin/dashboard").status()).isEqualTo(403);
        assertThat(anonymous.post("/api/v1/staff/login", Map.of("phone", DemoData.STAFF_PHONE, "password", "nope-nope"))
                .status()).isEqualTo(401);
        // A customer's password doesn't open the admin area.
        assertThat(anonymous.post("/api/v1/staff/login",
                Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD)).status()).isEqualTo(401);

        assertThat(admin.get("/api/v1/staff/me").body().get("staff").get("name").asString()).startsWith("Farhana");
        JsonNode dashboard = admin.get("/api/v1/admin/dashboard").body();
        assertThat(dashboard.get("week")).hasSize(7);
        assertThat(dashboard.get("open").has("CONFIRMED")).isTrue();
        assertThat(dashboard.get("topPlants").size()).isGreaterThan(0);
        // Staff aren't customers: no account pages.
        assertThat(admin.get("/api/v1/account/profile").status()).isEqualTo(401);

        jdbc.sql("update staff set active = false where phone = :p").param("p", DemoData.STAFF_PHONE).update();
        try {
            assertThat(admin.get("/api/v1/admin/dashboard").status()).isEqualTo(401);
        } finally {
            jdbc.sql("update staff set active = true where phone = :p").param("p", DemoData.STAFF_PHONE).update();
        }
    }

    @Test
    void anOrderIsConfirmedShippedWithACourierAndDelivered() {
        long zz = variant("zz-plant", "SMALL");
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("variantId", zz, "quantity", 2));
        JsonNode placed = place(shopper, "COD", "OUTSIDE");
        String number = placed.get("number").asString();

        JsonNode open = admin.get("/api/v1/admin/orders?filter=OPEN&zone=OUTSIDE&q=" + number).body();
        assertThat(open.get("items").get(0).get("number").asString()).isEqualTo(number);
        assertThat(admin.get("/api/v1/admin/orders?zone=DHAKA&q=" + number).body().get("items")).isEmpty();
        JsonNode detail = admin.get("/api/v1/admin/orders/" + number).body();
        assertThat(detail.get("next").toString()).contains("CONFIRMED", "CANCELLED");
        assertThat(detail.get("skus").get(0).asString()).isNotBlank();

        assertThat(admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "SHIPPED")).status()).isEqualTo(409);
        admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "CONFIRMED"));
        admin.post("/api/v1/admin/orders/" + number + "/notes", Map.of("note", "Customer asked for a call before delivery"));
        JsonNode shipped = admin.post("/api/v1/admin/orders/" + number + "/status",
                Map.of("status", "SHIPPED", "courier", "Sundarban Courier", "trackingCode", "SCS-55120")).body();
        assertThat(shipped.get("order").get("courier").asString()).isEqualTo("Sundarban Courier");
        // The shopper sees who is bringing it.
        JsonNode theirs = shopper.get("/api/v1/orders/" + number + "?token=" + placed.get("token").asString()).body();
        assertThat(theirs.get("trackingCode").asString()).isEqualTo("SCS-55120");

        JsonNode done = admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "DELIVERED")).body();
        assertThat(done.get("order").get("status").asString()).isEqualTo("DELIVERED");
        assertThat(done.get("order").get("paymentStatus").asString()).isEqualTo("PAID");
        assertThat(done.get("order").get("events").toString()).contains("Cash collected", "call before delivery",
                "With Sundarban Courier (SCS-55120)");
        assertThat(jdbc.sql("""
                        select count(*) from order_events e join orders o on o.id = e.order_id
                         where o.number = :n and e.actor like 'staff:Farhana%'""")
                .param("n", number).query(Integer.class).single()).isEqualTo(4);
        assertThat(done.get("next")).isEmpty();
    }

    @Test
    void cancellingAPaidOrderGivesStockBackAndShowsARefundDue() throws Exception {
        long fig = variant("fiddle-leaf-fig", "MEDIUM");
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("variantId", fig, "quantity", 1));
        String number = place(shopper, "ONLINE", "DHAKA").get("number").asString();
        String tran = number + "-1";
        String val = gateway.pay(tran, gateway.session(tran).get("total_amount"), "BDT", "bKash");
        HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + port + "/api/v1/payments/sslcommerz/ipn"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("tran_id=" + tran + "&status=VALID&val_id=" + val)).build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(stock(fig)).isEqualTo(49);

        JsonNode cancelled = admin.post("/api/v1/admin/orders/" + number + "/status",
                Map.of("status", "CANCELLED", "note", "Leaves damaged in the nursery")).body();
        assertThat(cancelled.get("refundDue").asBoolean()).isTrue();
        assertThat(cancelled.get("attempts").get(0).get("cardType").asString()).isEqualTo("bKash");
        assertThat(stock(fig)).isEqualTo(50);
        assertThat(admin.get("/api/v1/admin/orders?filter=REFUND_DUE").body().get("items").toString()).contains(number);

        assertThat(admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", " ")).status()).isEqualTo(400);
        JsonNode refunded = admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", "RF-88231")).body();
        assertThat(refunded.get("order").get("paymentStatus").asString()).isEqualTo("REFUNDED");
        assertThat(refunded.get("refundReference").asString()).isEqualTo("RF-88231");
        assertThat(admin.post("/api/v1/admin/orders/" + number + "/refund", Map.of("reference", "again")).status()).isEqualTo(409);
    }

    @Test
    void aReturnedParcelCanStayOutOfStock() {
        long rubber = variant("rubber-plant", "SMALL");
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("variantId", rubber, "quantity", 1));
        String number = place(shopper, "COD", "OUTSIDE").get("number").asString();
        admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "CONFIRMED"));
        admin.post("/api/v1/admin/orders/" + number + "/status", Map.of("status", "SHIPPED"));
        JsonNode back = admin.post("/api/v1/admin/orders/" + number + "/status",
                Map.of("status", "CANCELLED", "restock", false, "note", "Courier returned it wilted")).body();
        assertThat(back.get("order").get("events").toString()).contains("Plants not returned to stock");
        assertThat(back.get("refundDue").asBoolean()).isFalse();
        assertThat(stock(rubber)).isEqualTo(49);
    }

    Map<String, Object> plant(long category) {
        Map<String, Object> plant = new HashMap<>();
        plant.put("name", "Dwarf Kamini");
        plant.put("localName", "কামিনী");
        plant.put("botanicalName", "Murraya paniculata");
        plant.put("categoryId", category);
        plant.put("description", "Glossy leaves and sweet white flowers after rain.");
        plant.put("details", "Flowers several times a year\nGood as a hedge");
        plant.put("light", "FULL_SUN");
        plant.put("water", "MEDIUM");
        plant.put("careLevel", "EASY");
        plant.put("placement", "OUTDOOR");
        plant.put("matureHeight", "1.5–3 m");
        plant.put("growthRate", "MEDIUM");
        plant.put("status", "ACTIVE");
        plant.put("planters", new ArrayList<>(List.of(
                new HashMap<>(Map.of("name", "Grow bag", "kind", "GROW_BAG", "hex", "#2F3B2F")),
                new HashMap<>(Map.of("name", "Clay pot", "kind", "CLAY", "hex", "#B5653A")))));
        List<Map<String, Object>> variants = new ArrayList<>();
        variants.add(new HashMap<>(Map.of("planter", 0, "size", "SMALL", "sku", "KAM-S-GB", "price", 350, "stock", 20)));
        variants.add(new HashMap<>(Map.of("planter", 0, "size", "MEDIUM", "sku", "KAM-M-GB", "price", 650, "stock", 8)));
        variants.add(new HashMap<>(Map.of("planter", 1, "size", "MEDIUM", "sku", "KAM-M-CL", "price", 900,
                "compareAtPrice", 850, "stock", 5)));
        plant.put("variants", variants);
        return plant;
    }

    @Test
    @SuppressWarnings("unchecked")
    void plantsAreEditedWithPlantersSizesAndPictures() {
        long flowering = jdbc.sql("select id from categories where slug = 'flowering-trees'").query(Long.class).single();
        long gifts = jdbc.sql("select id from collections where slug = 'gifts-that-grow'").query(Long.class).single();
        Map<String, Object> plant = plant(flowering);
        Response invalid = admin.post("/api/v1/admin/products", plant);
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().get("fields").has("variants.2.compareAtPrice")).isTrue();

        List<Map<String, Object>> variants = (List<Map<String, Object>>) plant.get("variants");
        variants.get(2).put("compareAtPrice", 1100);
        variants.add(new HashMap<>(Map.of("planter", 1, "size", "MEDIUM", "sku", "KAM-M-CL2", "price", 950)));
        Response twice = admin.post("/api/v1/admin/products", plant);
        assertThat(twice.body().get("fields").get("variants.3.size").asString()).contains("already has this size");
        variants.remove(3);

        plant.put("collectionIds", List.of(gifts));
        JsonNode created = admin.post("/api/v1/admin/products", plant).body();
        long id = created.get("id").asLong();
        assertThat(created.get("slug").asString()).isEqualTo("dwarf-kamini");
        assertThat(created.get("variants").get(0).get("height").asString()).isEqualTo("1–2 ft");
        assertThat(created.get("collectionIds").get(0).asLong()).isEqualTo(gifts);
        JsonNode shop = new ApiClient(port).get("/api/v1/products/dwarf-kamini").body();
        assertThat(shop.get("planters")).hasSize(2);
        assertThat(shop.get("botanicalName").asString()).isEqualTo("Murraya paniculata");

        // Swap the two grow-bag sizes in one save, drop the clay-pot option and its planter, add a ceramic one.
        long growBag = created.get("planters").get(0).get("id").asLong();
        List<Map<String, Object>> planters = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", growBag, "name", "Grow bag", "kind", "GROW_BAG", "hex", "#2F3B2F")),
                new HashMap<>(Map.of("name", "White ceramic", "kind", "CERAMIC", "hex", "#F2EFE8"))));
        JsonNode small = created.get("variants").get(0);
        JsonNode medium = created.get("variants").get(1);
        List<Map<String, Object>> next = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", small.get("id").asLong(), "planter", 0, "size", "MEDIUM", "sku", "KAM-S-GB",
                        "price", 640, "stock", 20)),
                new HashMap<>(Map.of("id", medium.get("id").asLong(), "planter", 0, "size", "SMALL", "sku", "KAM-M-GB",
                        "price", 340, "stock", 8)),
                new HashMap<>(Map.of("planter", 1, "size", "SMALL", "sku", "KAM-S-CE", "price", 1200, "stock", 4,
                        "dhakaOnly", true))));
        plant.put("planters", planters);
        plant.put("variants", next);
        JsonNode updated = admin.put("/api/v1/admin/products/" + id, plant).body();
        assertThat(updated.get("planters")).hasSize(2);
        assertThat(updated.get("planters").get(1).get("name").asString()).isEqualTo("White ceramic");
        assertThat(updated.get("variants")).hasSize(3);
        assertThat(updated.get("variants").get(0).get("size").asString()).isEqualTo("SMALL");
        assertThat(updated.get("variants").get(0).get("price").asInt()).isEqualTo(340);
        assertThat(updated.get("variants").get(2).get("dhakaOnly").asBoolean()).isTrue();

        // A planter on sale needs a size.
        List<Map<String, Object>> lonely = new ArrayList<>(planters);
        lonely.add(new HashMap<>(Map.of("name", "Clay pot", "kind", "CLAY", "hex", "#B5653A")));
        plant.put("planters", lonely);
        assertThat(admin.put("/api/v1/admin/products/" + id, plant).body().get("fields").has("planters.2.name")).isTrue();
        plant.put("planters", planters);

        next.get(2).put("sku", jdbc.sql("select sku from variants v join products p on p.id = v.product_id where p.slug = 'neem' limit 1")
                .query(String.class).single());
        assertThat(admin.put("/api/v1/admin/products/" + id, plant).status()).isEqualTo(409);
        next.get(2).put("sku", "KAM-S-CE");

        // Pictures: one for every planter, one for the ceramic pot only.
        long ceramic = updated.get("planters").get(1).get("id").asLong();
        JsonNode one = admin.upload("/api/v1/admin/products/" + id + "/images", "file", "kamini.png", "image/png", PNG).body();
        String url = one.get("images").get(0).get("url").asString();
        assertThat(url).startsWith("/media/products/").endsWith(".png");
        assertThat(admin.status(url)).isEqualTo(200);
        JsonNode two = admin.upload("/api/v1/admin/products/" + id + "/images?planterId=" + ceramic + "&alt=In+a+white+pot",
                "file", "pot.png", "image/png", PNG).body();
        long second = two.get("images").get(1).get("id").asLong();
        assertThat(two.get("images").get(1).get("planterId").asLong()).isEqualTo(ceramic);
        JsonNode moved = admin.post("/api/v1/admin/products/" + id + "/images/" + second + "/move", Map.of("direction", -1)).body();
        assertThat(moved.get("images").get(0).get("id").asLong()).isEqualTo(second);
        Response svg = admin.upload("/api/v1/admin/products/" + id + "/images", "file", "x.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes());
        assertThat(svg.status()).isEqualTo(400);
        long other = jdbc.sql("select id from planters where product_id <> :p limit 1").param("p", id).query(Long.class).single();
        assertThat(admin.upload("/api/v1/admin/products/" + id + "/images?planterId=" + other, "file", "a.png", "image/png", PNG)
                .status()).isEqualTo(400);
        JsonNode shown = new ApiClient(port).get("/api/v1/products/dwarf-kamini").body();
        JsonNode ceramicOption = StreamSupport.stream(shown.get("planters").spliterator(), false)
                .filter(p -> p.get("name").asString().equals("White ceramic")).findFirst().orElseThrow();
        assertThat(ceramicOption.get("images")).hasSize(2);
        assertThat(shown.get("planters").get(0).get("images")).hasSize(1);
        assertThat(admin.delete("/api/v1/admin/products/" + id + "/images/" + second).body().get("images")).hasSize(1);

        plant.put("status", "ARCHIVED");
        assertThat(admin.put("/api/v1/admin/products/" + id, plant).status()).isEqualTo(200);
        assertThat(new ApiClient(port).get("/api/v1/products/dwarf-kamini").status()).isEqualTo(404);
        assertThat(new ApiClient(port).post("/api/v1/admin/products", plant).status()).isEqualTo(401);
    }

    @Test
    void stockIsListedPerOptionAndSet() {
        long arjun = variant("arjun", "SMALL");
        jdbc.sql("update variants set stock = 2 where id = :id").param("id", arjun).update();
        JsonNode low = admin.get("/api/v1/admin/stock?low=true").body();
        assertThat(low.toString()).contains("\"variantId\":" + arjun);
        JsonNode set = admin.patch("/api/v1/admin/variants/" + arjun + "/stock", Map.of("stock", 120)).body();
        assertThat(set.get("stock").asInt()).isEqualTo(120);
        assertThat(set.get("planter").asString()).isNotBlank();
        assertThat(admin.patch("/api/v1/admin/variants/" + arjun + "/stock", Map.of("stock", -1)).status()).isEqualTo(400);
        assertThat(admin.get("/api/v1/admin/products?low=true").body().get("counts").get("ACTIVE").asInt()).isGreaterThan(20);
    }

    @Test
    void couponsAndDeliveryFees() {
        Map<String, Object> coupon = new HashMap<>(Map.of("code", "boishakh200", "description", "৳200 off for Pohela Boishakh",
                "kind", "FIXED", "value", 200, "minSubtotal", 1000));
        JsonNode created = admin.post("/api/v1/admin/coupons", coupon).body();
        assertThat(created.get("code").asString()).isEqualTo("BOISHAKH200");
        assertThat(admin.post("/api/v1/admin/coupons", coupon).status()).isEqualTo(409);

        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("variantId", variant("mahogany", "MEDIUM"), "quantity", 5));
        assertThat(shopper.post("/api/v1/cart/coupon", Map.of("code", "BOISHAKH200")).body().get("discount").asInt()).isEqualTo(200);
        coupon.put("active", false);
        admin.put("/api/v1/admin/coupons/" + created.get("id").asLong(), coupon);
        assertThat(shopper.get("/api/v1/cart").body().get("couponProblem").asString()).isNotBlank();

        try {
            assertThat(admin.put("/api/v1/admin/delivery", Map.of("insideDhaka", -5, "outsideDhaka", 150, "freeOver", 4000))
                    .body().get("fields").has("insideDhaka")).isTrue();
            JsonNode fees = admin.put("/api/v1/admin/delivery", Map.of("insideDhaka", 80, "outsideDhaka", 150, "freeOver", 99999)).body();
            assertThat(fees.get("updatedBy").asString()).startsWith("staff:Farhana");
            assertThat(new ApiClient(port).get("/api/v1/delivery").body().get("outsideDhaka").asInt()).isEqualTo(150);
            Response quoted = shopper.post("/api/v1/checkout/quote", Map.of("zone", "OUTSIDE"));
            assertThat(quoted.status()).as(quoted.body().toString()).isEqualTo(200);
            JsonNode quote = quoted.body();
            assertThat(quote.get("shippingFee").asInt()).isEqualTo(150);
            assertThat(shopper.get("/api/v1/cart").body().get("freeDeliveryOver").asInt()).isEqualTo(99999);
        } finally {
            jdbc.sql("delete from delivery_settings").update();
        }
        assertThat(new ApiClient(port).get("/api/v1/delivery").body().get("insideDhaka").asInt()).isEqualTo(100);
    }

    @Test
    void categoriesAndCollectionsAreEdited() {
        JsonNode tree = admin.get("/api/v1/admin/categories").body();
        assertThat(tree).hasSize(3);
        long trees = StreamSupport.stream(tree.spliterator(), false).filter(c -> c.get("slug").asString().equals("trees"))
                .findFirst().orElseThrow().get("id").asLong();
        assertThat(admin.post("/api/v1/admin/categories", Map.of("name", "X", "parentId", trees)).status()).isEqualTo(400);
        JsonNode added = admin.post("/api/v1/admin/categories",
                Map.of("name", "Medicinal plants", "parentId", trees, "description", "Tulsi, aloe and neem.")).body();
        JsonNode medicinal = StreamSupport.stream(added.spliterator(), false).filter(c -> c.get("id").asLong() == trees)
                .findFirst().orElseThrow().get("children").get(4);
        assertThat(medicinal.get("slug").asString()).isEqualTo("medicinal-plants");
        long medicinalId = medicinal.get("id").asLong();
        JsonNode withPicture = admin.upload("/api/v1/admin/categories/" + medicinalId + "/image", "file", "m.png", "image/png", PNG).body();
        assertThat(withPicture.toString()).contains("/media/categories/");
        assertThat(admin.delete("/api/v1/admin/categories/" + trees).status()).isEqualTo(409);
        assertThat(admin.delete("/api/v1/admin/categories/" + medicinalId).status()).isEqualTo(200);

        JsonNode collections = admin.post("/api/v1/admin/collections",
                Map.of("name", "Balcony Herbs", "tagline", "Fresh leaves by the kitchen", "active", false)).body();
        JsonNode herbs = collections.get(collections.size() - 1);
        assertThat(herbs.get("slug").asString()).isEqualTo("balcony-herbs");
        assertThat(new ApiClient(port).get("/api/v1/collections/balcony-herbs").status()).isEqualTo(404);
        JsonNode moved = admin.post("/api/v1/admin/collections/" + herbs.get("id").asLong() + "/move", Map.of("direction", -1)).body();
        assertThat(moved.get(moved.size() - 2).get("slug").asString()).isEqualTo("balcony-herbs");
        assertThat(admin.post("/api/v1/admin/collections", Map.of("name", "Balcony Herbs")).status()).isEqualTo(409);
    }

    @Test
    void staffAreAddedSwitchedOffAndReset() {
        assertThat(admin.post("/api/v1/admin/staff", Map.of("name", "Jamal", "phone", "01312345670", "password", "short"))
                .status()).isEqualTo(400);
        admin.post("/api/v1/admin/staff", Map.of("name", "Jamal Gardener", "phone", "01312345670", "password", "start-here-123"));
        assertThat(admin.post("/api/v1/admin/staff", Map.of("name", "Again", "phone", "01312345670", "password", "start-here-123"))
                .status()).isEqualTo(409);
        long jamal = jdbc.sql("select id from staff where phone = '01312345670'").query(Long.class).single();

        ApiClient jamalsPhone = new ApiClient(port);
        assertThat(jamalsPhone.post("/api/v1/staff/login", Map.of("phone", "01312345670", "password", "start-here-123")).status())
                .isEqualTo(200);
        assertThat(jamalsPhone.post("/api/v1/admin/me/password",
                Map.of("currentPassword", "start-here-123", "newPassword", "jamal-own-pass")).status()).isEqualTo(204);

        admin.post("/api/v1/admin/staff/" + jamal + "/active", Map.of("active", false));
        assertThat(jamalsPhone.get("/api/v1/admin/dashboard").status()).isEqualTo(401);
        assertThat(new ApiClient(port).post("/api/v1/staff/login", Map.of("phone", "01312345670", "password", "jamal-own-pass"))
                .status()).isEqualTo(401);

        admin.post("/api/v1/admin/staff/" + jamal + "/active", Map.of("active", true));
        admin.post("/api/v1/admin/staff/" + jamal + "/password", Map.of("password", "manager-set-99"));
        assertThat(new ApiClient(port).post("/api/v1/staff/login", Map.of("phone", "01312345670", "password", "manager-set-99"))
                .status()).isEqualTo(200);

        long me = jdbc.sql("select id from staff where phone = :p").param("p", DemoData.STAFF_PHONE).query(Long.class).single();
        Response self = admin.post("/api/v1/admin/staff/" + me + "/active", Map.of("active", false));
        assertThat(self.status()).isEqualTo(409);
        assertThat(self.body().get("code").asString()).isEqualTo("staff.self");
    }
}
