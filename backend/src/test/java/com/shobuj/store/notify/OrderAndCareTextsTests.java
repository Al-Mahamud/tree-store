package com.shobuj.store.notify;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.DemoData;
import com.shobuj.store.support.IntegrationTest;
import com.shobuj.store.support.TextsSent;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class OrderAndCareTextsTests {

    /** The GSM 7-bit basic alphabet: anything else makes a text Unicode and only 70 characters long. */
    static final Pattern GSM = Pattern.compile("[A-Za-z0-9 @£$¥èéùìòÇ\\nØø\\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ!\"#¤%&'()*+,\\-./:;<=>?¡ÄÖÑÜ§¿äöñüà]*");

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    CareTexts care;

    ApiClient admin;

    @BeforeEach
    void signIn() {
        admin = new ApiClient(port);
        admin.post("/api/v1/staff/login", Map.of("phone", DemoData.STAFF_PHONE, "password", DemoData.STAFF_PASSWORD));
    }

    long variant(String slug, String size) {
        long id = jdbc.sql("""
                        select v.id from variants v join products p on p.id = v.product_id
                         where p.slug = :s and v.size = :size order by v.price, v.id limit 1""")
                .param("s", slug).param("size", size).query(Long.class).single();
        jdbc.sql("update variants set stock = 50 where id = :id").param("id", id).update();
        return id;
    }

    JsonNode place(String phone, String slug, Boolean careTexts) {
        ApiClient shopper = new ApiClient(port);
        shopper.post("/api/v1/cart/items", Map.of("variantId", variant(slug, "SMALL"), "quantity", 1));
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Mitu Rahman");
        body.put("phone", phone);
        body.put("address", "Flat 2C, House 8, Road 4");
        body.put("area", "Uttara");
        body.put("city", "Dhaka");
        body.put("zone", "DHAKA");
        body.put("paymentMethod", "COD");
        if (careTexts != null) {
            body.put("careTexts", careTexts);
        }
        ApiClient.Response r = shopper.post("/api/v1/checkout", body);
        assertThat(r.status()).isEqualTo(201);
        return r.body();
    }

    void move(String number, String status, Map<String, Object> extra) {
        Map<String, Object> body = new HashMap<>(extra);
        body.put("status", status);
        assertThat(admin.post("/api/v1/admin/orders/" + number + "/status", body).status()).isEqualTo(200);
    }

    static void assertOneSms(String text) {
        assertThat(text.length()).as(text).isLessThanOrEqualTo(160);
        assertThat(GSM.matcher(text).matches()).as("SMS alphabet: " + text).isTrue();
    }

    @Test
    void customersAreTextedWhenAnOrderIsPlacedSentAndCancelled() {
        String phone = "01556000101";
        String number = place(phone, "kagzi-lemon", null).get("number").asString();
        List<String> placed = TextsSent.bodies(jdbc, phone, "ORDER_PLACED");
        assertThat(placed).hasSize(1);
        assertThat(placed.get(0)).contains(number, "cash on delivery", "shop.test/track");
        assertOneSms(placed.get(0));

        move(number, "CONFIRMED", Map.of());
        assertThat(jdbc.sql("select count(*) from sms_messages where phone = :p").param("p", phone)
                .query(Long.class).single()).isEqualTo(1);
        move(number, "SHIPPED", Map.of("courier", "Sundarban Courier Service Limited Head Office", "trackingCode", "SCS-55021"));
        String shipped = TextsSent.bodies(jdbc, phone, "ORDER_SHIPPED").get(0);
        assertThat(shipped).contains(number, "Sundarban Courier", "SCS-55021", "keep Tk");
        assertOneSms(shipped);

        String second = place(phone, "money-plant", null).get("number").asString();
        move(second, "CANCELLED", Map.of("restock", true));
        String cancelled = TextsSent.bodies(jdbc, phone, "ORDER_CANCELLED").get(0);
        assertThat(cancelled).contains(second, "Call us");
        assertOneSms(cancelled);
    }

    @Test
    void deliveredOrdersGetThreeCareTextsAtTenInTheMorning() {
        String phone = "01556000202";
        String number = place(phone, "monstera-deliciosa", null).get("number").asString();
        move(number, "CONFIRMED", Map.of());
        move(number, "SHIPPED", Map.of());
        move(number, "DELIVERED", Map.of());
        long order = jdbc.sql("select id from orders where number = :n").param("n", number).query(Long.class).single();
        List<Instant> due = jdbc.sql("select due_at from care_reminders where order_id = :o order by step").param("o", order)
                .query((rs, n) -> rs.getTimestamp(1).toInstant()).list();
        assertThat(due).hasSize(3);
        LocalDate delivered = Instant.now().atZone(CareTexts.DHAKA).toLocalDate();
        assertThat(due.get(0).atZone(CareTexts.DHAKA).toLocalDate()).isEqualTo(delivered.plusDays(2));
        assertThat(due.get(0).atZone(CareTexts.DHAKA).toLocalTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(due.get(2).atZone(CareTexts.DHAKA).toLocalDate()).isEqualTo(delivered.plusDays(25));

        // Day 2, 11am: the first text goes out.
        Instant day2 = due.get(0).plusSeconds(3600);
        care.sendDue(day2);
        List<String> texts = TextsSent.bodies(jdbc, phone, "CARE_REMINDER");
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0)).contains("Monstera", "settling in");
        // Day 14 at 11pm: too late in the evening, it waits for the morning.
        care.sendDue(due.get(1).plusSeconds(13 * 3600));
        assertThat(TextsSent.bodies(jdbc, phone, "CARE_REMINDER")).hasSize(1);
        care.sendDue(due.get(1).plusSeconds(23 * 3600));
        assertThat(TextsSent.bodies(jdbc, phone, "CARE_REMINDER")).hasSize(2);
        // Turned off on the order page: the guarantee text isn't sent.
        String token = jdbc.sql("select access_token from orders where id = :o").param("o", order).query(String.class).single();
        ApiClient guest = new ApiClient(port);
        assertThat(guest.put("/api/v1/orders/" + number + "/care-texts?token=wrong", Map.of("on", false)).status()).isEqualTo(404);
        assertThat(guest.put("/api/v1/orders/" + number + "/care-texts?token=" + token, Map.of("on", false)).status()).isEqualTo(200);
        assertThat(guest.get("/api/v1/orders/" + number + "?token=" + token).body().get("careTexts").asBoolean()).isFalse();
        care.sendDue(due.get(2).plusSeconds(60));
        assertThat(TextsSent.bodies(jdbc, phone, "CARE_REMINDER")).hasSize(2);
        assertThat(jdbc.sql("select outcome from care_reminders where order_id = :o and step = 3").param("o", order)
                .query(String.class).single()).isEqualTo("OFF");
    }

    @Test
    void careTextsCanBeRefusedAtCheckoutAndOldOnesAreDropped() {
        String phone = "01556000303";
        String off = place(phone, "snake-plant", false).get("number").asString();
        String on = place(phone, "zz-plant", null).get("number").asString();
        for (String n : List.of(off, on)) {
            move(n, "CONFIRMED", Map.of());
            move(n, "SHIPPED", Map.of());
            move(n, "DELIVERED", Map.of());
        }
        Instant first = jdbc.sql("""
                        select min(r.due_at) from care_reminders r join orders o on o.id = r.order_id where o.number = :n""")
                .param("n", on).query(Timestamp.class).single().toInstant();
        // Three days overdue (the app was down): dropped rather than sent late.
        care.sendDue(first.plusSeconds(3 * 86_400));
        assertThat(TextsSent.bodies(jdbc, phone, "CARE_REMINDER")).isEmpty();
        assertThat(jdbc.sql("""
                        select r.outcome from care_reminders r join orders o on o.id = r.order_id
                         where o.number in (:n) and r.step = 1 order by o.id""")
                .param("n", List.of(off, on)).query(String.class).list()).containsExactly("OFF", "LATE");
    }

    @Test
    void everyCareTextFitsOneSms() {
        List<CareTexts.Plant> plants = jdbc.sql("select name, light, water, placement from products")
                .query((rs, n) -> new CareTexts.Plant(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .list();
        assertThat(plants).isNotEmpty();
        plants = new java.util.ArrayList<>(plants);
        plants.add(new CareTexts.Plant("A very long plant name that goes on and on", "BRIGHT", "HIGH", "INDOOR"));
        for (CareTexts.Plant p : plants) {
            for (int step = 1; step <= 3; step++) {
                assertOneSms(CareTexts.text(step, p, "SB100234", "shobuj-nursery.com.bd"));
            }
        }
    }
}
