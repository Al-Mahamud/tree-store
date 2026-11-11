package com.shobuj.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.shobuj.store.support.ApiClient;
import com.shobuj.store.support.ApiClient.Response;
import com.shobuj.store.support.IntegrationTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;

@IntegrationTest
class CatalogApiTests {

    @LocalServerPort
    int port;

    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
    }

    @Test
    void categoryTreeCountsIncludeSubCategories() {
        JsonNode tree = api.get("/api/v1/categories").body();
        assertThat(names(tree)).containsExactly("Trees", "Indoor plants", "Bonsai");
        assertThat(tree.get(0).get("productCount").asInt()).isEqualTo(19);
        assertThat(tree.get(1).get("productCount").asInt()).isEqualTo(6);
        assertThat(tree.get(2).get("productCount").asInt()).isEqualTo(2);
        assertThat(tree.get(0).get("image").asString()).startsWith("/media/demo/");
    }

    @Test
    void lightFilterAndFacet() {
        JsonNode page = api.get("/api/v1/products?light=low").body();
        assertThat(names(page.get("items"))).containsExactlyInAnyOrder("Snake Plant", "ZZ Plant");
        JsonNode low = find(page.get("facets").get("light"), "LOW");
        assertThat(low.get("selected").asBoolean()).isTrue();
        // The light facet ignores its own filter, so other choices still show their counts.
        assertThat(find(page.get("facets").get("light"), "FULL_SUN").get("count").asInt()).isGreaterThan(10);
        assertThat(api.get("/api/v1/products?light=moonlight").status()).isEqualTo(400);
    }

    @Test
    void indoorIncludesPlantsForBothPlaces() {
        List<String> names = names(api.get("/api/v1/products?placement=indoor&pageSize=48").body().get("items"));
        assertThat(names).contains("Snake Plant", "Areca Palm", "Beli Jasmine").doesNotContain("Amrapali Mango");
    }

    @Test
    void petFriendlyAndCareLevel() {
        assertThat(names(api.get("/api/v1/products?petFriendly=true").body().get("items")))
                .containsExactlyInAnyOrder("Areca Palm", "Beli Jasmine");
        JsonNode expert = api.get("/api/v1/products?care=expert").body();
        assertThat(names(expert.get("items"))).containsExactlyInAnyOrder("Fiddle Leaf Fig", "Bougainvillea Bonsai");
    }

    @Test
    void searchFindsEnglishBanglaAndBotanicalNames() {
        assertThat(names(api.get("/api/v1/products?q=mang").body().get("items"))).contains("Amrapali Mango");
        assertThat(names(api.get("/api/v1/products?q=" + enc("আম")).body().get("items"))).contains("Amrapali Mango");
        assertThat(names(api.get("/api/v1/products?q=ficus&pageSize=48").body().get("items")))
                .contains("Fiddle Leaf Fig", "Rubber Plant", "Ficus Bonsai");
        assertThat(api.get("/api/v1/products?q=" + enc("50%_\\")).status()).isEqualTo(200);
    }

    @Test
    void pricesAreTheCheapestOption() {
        JsonNode items = api.get("/api/v1/products?sort=price_asc&pageSize=48").body().get("items");
        int previous = 0;
        for (JsonNode item : items) {
            assertThat(item.get("fromPrice").asInt()).isGreaterThanOrEqualTo(previous);
            previous = item.get("fromPrice").asInt();
        }
        JsonNode cheap = api.get("/api/v1/products?maxPrice=200").body();
        for (JsonNode item : cheap.get("items")) {
            assertThat(item.get("fromPrice").asInt()).isLessThanOrEqualTo(200);
        }
        assertThat(cheap.get("total").asInt()).isPositive();
    }

    @Test
    void plantPageHasPlantersOptionsAndCareFacts() {
        Response response = api.get("/api/v1/products/amrapali-mango");
        assertThat(response.status()).isEqualTo(200);
        JsonNode plant = response.body();
        assertThat(plant.get("localName").asString()).isEqualTo("আম্রপালি আম");
        assertThat(names(plant.get("breadcrumb"))).containsExactly("Trees", "Fruit trees");
        assertThat(plant.get("care").get("light").asString()).isEqualTo("FULL_SUN");
        assertThat(plant.get("care").get("fruiting").asString()).contains("2–3 years");
        assertThat(names(plant.get("planters"))).containsExactly("Grow bag", "Clay pot");
        JsonNode bag = plant.get("planters").get(0);
        JsonNode clay = plant.get("planters").get(1);
        assertThat(bag.get("images")).hasSize(2);
        List<String> sizes = new ArrayList<>();
        bag.get("options").forEach(o -> sizes.add(o.get("size").asString()));
        assertThat(sizes).containsExactly("SMALL", "MEDIUM", "LARGE");
        assertThat(bag.get("options").get(0).get("price").asInt()).isEqualTo(450);
        assertThat(clay.get("options").get(0).get("price").asInt()).isEqualTo(700);
        assertThat(bag.get("options").get(2).get("dhakaOnly").asBoolean()).isTrue();
        assertThat(bag.get("options").get(0).get("dhakaOnly").asBoolean()).isFalse();
    }

    @Test
    void unknownPlantIsNotFound() {
        Response response = api.get("/api/v1/products/no-such-plant");
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.body().get("code").asString()).isEqualTo("product.not_found");
    }

    @Test
    void homeRailsAndCollections() {
        JsonNode home = api.get("/api/v1/home").body();
        assertThat(home.get("bestSellers").get(0).get("name").asString()).isEqualTo("Money Plant");
        for (JsonNode fruit : home.get("fruitTrees")) {
            assertThat(fruit.get("fruiting").isNull()).isFalse();
        }
        assertThat(names(home.get("collections"))).containsExactly("Monsoon Planting", "Gifts that Grow", "Rooftop Garden");
        JsonNode gifts = api.get("/api/v1/collections/gifts-that-grow").body();
        assertThat(gifts.get("productCount").asInt())
                .isEqualTo(api.get("/api/v1/products?collection=gifts-that-grow").body().get("total").asInt());
    }

    @Test
    void relatedPlantsAndImages() {
        JsonNode related = api.get("/api/v1/products/kagzi-lemon/related").body();
        assertThat(names(related)).doesNotContain("Kagzi Lemon").isNotEmpty();
        assertThat(related.get(0).get("category").asString()).isEqualTo("Fruit trees");
        String image = api.get("/api/v1/products/kagzi-lemon").body()
                .get("planters").get(0).get("images").get(0).get("url").asString();
        assertThat(api.get(image).status()).isEqualTo(200);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static JsonNode find(JsonNode facet, String value) {
        for (JsonNode f : facet) {
            if (f.get("value").asString().equals(value)) {
                return f;
            }
        }
        throw new AssertionError("No facet value " + value);
    }

    private static List<String> names(JsonNode nodes) {
        List<String> names = new ArrayList<>();
        nodes.forEach(n -> names.add(n.get("name").asString()));
        return names;
    }
}
