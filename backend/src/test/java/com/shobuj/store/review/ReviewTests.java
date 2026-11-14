package com.shobuj.store.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.DemoData;
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
class ReviewTests {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    ApiClient nadia;

    @BeforeEach
    void signIn() {
        nadia = new ApiClient(port);
        assertThat(nadia.post("/api/v1/auth/login", Map.of("phone", DemoData.DEMO_PHONE, "password", DemoData.DEMO_PASSWORD))
                .status()).isEqualTo(200);
        // Each test starts from the demo reviews.
        jdbc.sql("""
                delete from reviews where customer_id = (select id from customers where phone = :p)
                   and product_id in (select id from products where slug in ('snake-plant', 'neem'))""")
                .param("p", DemoData.DEMO_PHONE).update();
        jdbc.sql("update reviews set status = 'PUBLISHED'").update();
        jdbc.sql("""
                update products p set review_count = (select count(*) from reviews r where r.product_id = p.id and r.status = 'PUBLISHED'),
                       rating = (select round(avg(rating)::numeric, 1) from reviews r where r.product_id = p.id and r.status = 'PUBLISHED')""")
                .update();
    }

    Map<String, Object> review(int rating, String title, String body) {
        Map<String, Object> m = new HashMap<>();
        m.put("rating", rating);
        m.put("title", title);
        m.put("body", body);
        return m;
    }

    @Test
    void productPagesShowPublishedReviewsWithASummary() {
        JsonNode page = new ApiClient(port).get("/api/v1/products/amrapali-mango/reviews").body();
        JsonNode summary = page.get("summary");
        assertThat(summary.get("count").asInt()).isEqualTo(5);
        assertThat(summary.get("average").asDouble()).isEqualTo(4.8);
        assertThat(summary.get("stars").get(0).asInt()).isEqualTo(4);
        assertThat(summary.get("withPhotos").asInt()).isEqualTo(2);
        assertThat(page.get("items").get(0).get("author").asString()).isEqualTo("Nadia R.");

        JsonNode photosFirst = new ApiClient(port).get("/api/v1/products/amrapali-mango/reviews?sort=PHOTOS").body();
        assertThat(photosFirst.get("items").get(0).get("photos")).isNotEmpty();
        JsonNode lowest = new ApiClient(port).get("/api/v1/products/amrapali-mango/reviews?sort=LOWEST").body();
        assertThat(lowest.get("items").get(0).get("rating").asInt()).isEqualTo(4);

        JsonNode product = new ApiClient(port).get("/api/v1/products/amrapali-mango").body();
        assertThat(product.get("rating").asDouble()).isEqualTo(4.8);
        assertThat(product.get("reviewCount").asInt()).isEqualTo(5);
        JsonNode cards = new ApiClient(port).get("/api/v1/products?q=amrapali").body().get("items");
        assertThat(cards.get(0).get("reviewCount").asInt()).isEqualTo(5);
    }

    @Test
    void onlyDeliveredPlantsCanBeReviewedOncePerCustomer() {
        JsonNode mine = nadia.get("/api/v1/account/reviews").body();
        assertThat(mine.get("mine")).hasSize(1);
        assertThat(mine.get("toReview").toString()).contains("snake-plant", "money-plant", "neem")
                .doesNotContain("amrapali-mango");

        assertThat(nadia.get("/api/v1/account/reviews/for/krishnachura").body().get("canReview").asBoolean()).isFalse();
        assertThat(nadia.put("/api/v1/account/reviews/for/krishnachura", review(5, null, "Never bought this one")).status())
                .isEqualTo(409);
        assertThat(nadia.get("/api/v1/account/reviews/for/snake-plant").body().get("canReview").asBoolean()).isTrue();

        ApiClient.Response bad = nadia.put("/api/v1/account/reviews/for/snake-plant", review(0, null, "short"));
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.body().get("fields").has("rating")).isTrue();
        assertThat(bad.body().get("fields").has("body")).isTrue();

        JsonNode saved = nadia.put("/api/v1/account/reviews/for/snake-plant",
                review(4, "Happy in the bathroom", "Doing well in a bathroom with a small window.")).body();
        assertThat(saved.get("bought").asString()).startsWith("Small · ").endsWith("ceramic");
        assertThat(new ApiClient(port).get("/api/v1/products/snake-plant").body().get("reviewCount").asInt()).isEqualTo(3);

        // Writing again changes the same review.
        nadia.put("/api/v1/account/reviews/for/snake-plant", review(5, null, "Even better after a month: two new pups."));
        JsonNode page = new ApiClient(port).get("/api/v1/products/snake-plant/reviews").body();
        assertThat(page.get("summary").get("count").asInt()).isEqualTo(3);
        assertThat(page.get("items").get(0).get("body").asString()).startsWith("Even better");
        assertThat(page.get("items").get(0).get("title").isNull()).isTrue();

        // Guests can read but not write.
        ApiClient guest = new ApiClient(port);
        assertThat(guest.put("/api/v1/account/reviews/for/snake-plant", review(5, null, "Anonymous praise here")).status())
                .isEqualTo(401);

        long id = saved.get("id").asLong();
        assertThat(nadia.delete("/api/v1/account/reviews/" + id).status()).isEqualTo(204);
        assertThat(new ApiClient(port).get("/api/v1/products/snake-plant").body().get("reviewCount").asInt()).isEqualTo(2);
    }

    @Test
    void reviewsTakeUpToThreePhotos() {
        long id = nadia.put("/api/v1/account/reviews/for/neem", review(3, null, "Growing slowly but steadily.")).body()
                .get("id").asLong();
        for (int i = 0; i < 3; i++) {
            assertThat(nadia.upload("/api/v1/account/reviews/" + id + "/photos", "file", "p.png", "image/png", PNG).status())
                    .isEqualTo(200);
        }
        ApiClient.Response fourth = nadia.upload("/api/v1/account/reviews/" + id + "/photos", "file", "p.png", "image/png", PNG);
        assertThat(fourth.status()).isEqualTo(409);
        assertThat(nadia.upload("/api/v1/account/reviews/" + id + "/photos", "file", "p.txt", "image/png",
                "not a picture at all".getBytes()).status()).isIn(400, 409);
        JsonNode photos = new ApiClient(port).get("/api/v1/products/neem/reviews?sort=PHOTOS").body().get("items").get(0).get("photos");
        assertThat(photos).hasSize(3);
        assertThat(photos.get(0).get("url").asString()).startsWith("/media/reviews/");

        // Someone else can't touch them.
        ApiClient other = new ApiClient(port);
        assertThat(other.delete("/api/v1/account/reviews/" + id + "/photos/" + photos.get(0).get("id").asLong()).status())
                .isEqualTo(401);
        JsonNode after = nadia.delete("/api/v1/account/reviews/" + id + "/photos/" + photos.get(0).get("id").asLong()).body();
        assertThat(after.get("photos")).hasSize(2);
    }

    @Test
    void staffCanHideAndReplyToReviews() {
        ApiClient admin = new ApiClient(port);
        admin.post("/api/v1/staff/login", Map.of("phone", DemoData.STAFF_PHONE, "password", DemoData.STAFF_PASSWORD));
        assertThat(nadia.get("/api/v1/admin/reviews").status()).isEqualTo(403);

        JsonNode low = admin.get("/api/v1/admin/reviews?filter=LOW").body();
        assertThat(low.get("items")).isNotEmpty();
        JsonNode neem = low.get("items").get(0);
        assertThat(neem.get("rating").asInt()).isLessThanOrEqualTo(2);
        assertThat(neem.get("phone").asString()).startsWith("01");
        long id = neem.get("id").asLong();
        String slug = neem.get("slug").asString();
        int before = new ApiClient(port).get("/api/v1/products/" + slug).body().get("reviewCount").asInt();

        assertThat(admin.patch("/api/v1/admin/reviews/" + id, Map.of("status", "HIDDEN")).status()).isEqualTo(200);
        assertThat(new ApiClient(port).get("/api/v1/products/" + slug).body().get("reviewCount").asInt()).isEqualTo(before - 1);
        assertThat(admin.get("/api/v1/admin/reviews?filter=HIDDEN").body().get("counts").get("HIDDEN").asInt()).isEqualTo(1);
        assertThat(admin.patch("/api/v1/admin/reviews/" + id, Map.of("status", "GONE")).status()).isEqualTo(400);

        admin.patch("/api/v1/admin/reviews/" + id, Map.of("status", "PUBLISHED", "reply", "  Thanks, we'll call you.  "));
        JsonNode shown = new ApiClient(port).get("/api/v1/products/" + slug + "/reviews?sort=LOWEST").body().get("items").get(0);
        assertThat(shown.get("reply").asString()).isEqualTo("Thanks, we'll call you.");
        admin.patch("/api/v1/admin/reviews/" + id, Map.of("reply", ""));
        shown = new ApiClient(port).get("/api/v1/products/" + slug + "/reviews?sort=LOWEST").body().get("items").get(0);
        assertThat(shown.get("reply").isNull()).isTrue();
    }

    @Test
    void theSitemapListsWhatSearchEnginesShouldFind() {
        JsonNode map = new ApiClient(port).get("/api/v1/sitemap").body();
        assertThat(map.get("products").size()).isGreaterThan(20);
        assertThat(map.get("categories").toString()).contains("fruit-trees", "indoor");
        assertThat(map.get("collections").toString()).contains("monsoon-planting");
    }
}
