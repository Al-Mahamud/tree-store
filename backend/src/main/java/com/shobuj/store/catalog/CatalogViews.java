package com.shobuj.store.catalog;

import java.util.List;

/** What the catalog API returns. */
public final class CatalogViews {

    private CatalogViews() {
    }

    public record Swatch(String name, String kind, String hex) {
    }

    public record ProductCard(
            long id,
            String slug,
            String name,
            String localName,
            String category,
            int fromPrice,
            Integer compareAtPrice,
            boolean priceVaries,
            String image,
            String hoverImage,
            List<Swatch> planters,
            String light,
            String careLevel,
            boolean petFriendly,
            String fruiting,
            boolean isNew,
            boolean soldOut,
            /* Average of published reviews (null without any), and how many there are. */
            Double rating,
            int reviewCount) {
    }

    public record CategoryNode(long id, String name, String slug, String description, String image, int productCount,
                               List<CategoryNode> children) {
    }

    public record CategoryRef(String name, String slug) {
    }

    public record FacetValue(String value, String label, int count, boolean selected) {
    }

    public record PriceRange(int min, int max) {
    }

    public record Facets(List<FacetValue> light, List<FacetValue> care, List<FacetValue> placement,
                         List<FacetValue> sizes, int petFriendly, PriceRange price) {
    }

    public record ProductPage(List<ProductCard> items, int page, int pageSize, long total, Facets facets) {
    }

    public record Image(String url, String alt) {
    }

    public record Option(long variantId, String size, String sizeLabel, String height, String sku, int price,
                         Integer compareAtPrice, int stock, boolean dhakaOnly) {
    }

    public record PlanterOption(long id, String name, String kind, String hex, List<Image> images,
                                List<Option> options) {
    }

    public record CareFacts(String light, String water, String careLevel, String placement, boolean petFriendly,
                            String matureHeight, String growthRate, String fruiting) {
    }

    public record ProductDetail(
            long id,
            String slug,
            String name,
            String localName,
            String botanicalName,
            List<CategoryRef> breadcrumb,
            String description,
            List<String> details,
            CareFacts care,
            boolean isNew,
            List<PlanterOption> planters,
            Double rating,
            int reviewCount) {
    }

    public record CollectionView(long id, String name, String slug, String tagline, String description, String image,
                                 int productCount) {
    }

    public record Home(List<CollectionView> collections, List<CategoryNode> categories, List<ProductCard> bestSellers,
                       List<ProductCard> fruitTrees, List<ProductCard> newArrivals) {
    }
}
