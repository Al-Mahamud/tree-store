package com.shobuj.store.catalog;

import com.shobuj.store.catalog.CatalogViews.CategoryNode;
import com.shobuj.store.catalog.CatalogViews.CollectionView;
import com.shobuj.store.catalog.CatalogViews.Home;
import com.shobuj.store.catalog.CatalogViews.ProductCard;
import com.shobuj.store.catalog.CatalogViews.ProductDetail;
import com.shobuj.store.catalog.CatalogViews.ProductPage;
import com.shobuj.store.shared.ApiException;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class CatalogController {

    private static final int MAX_PAGE_SIZE = 48;

    private final CatalogService catalog;
    private final ProductSearch search;

    CatalogController(CatalogService catalog, ProductSearch search) {
        this.catalog = catalog;
        this.search = search;
    }

    @GetMapping("/home")
    Home home() {
        return catalog.home();
    }

    @GetMapping("/categories")
    List<CategoryNode> categories() {
        return catalog.categoryTree();
    }

    @GetMapping("/categories/{slug}")
    CategoryNode category(@PathVariable String slug) {
        return catalog.category(slug);
    }

    @GetMapping("/collections")
    List<CollectionView> collections() {
        return catalog.collections();
    }

    @GetMapping("/collections/{slug}")
    CollectionView collection(@PathVariable String slug) {
        return catalog.collection(slug);
    }

    /**
     * Plant grid. {@code light}, {@code care} and {@code size} take comma-separated values;
     * {@code sort} is one of recommended, newest, price_asc, price_desc, popular.
     */
    @GetMapping("/products")
    ProductPage products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String light,
            @RequestParam(required = false) String care,
            @RequestParam(required = false) String placement,
            @RequestParam(defaultValue = "false") boolean petFriendly,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "false") boolean sale,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("request.paging", "Page must be 0 or more and page size 1 to 48.");
        }
        ProductQuery.Sort order = Care.parse(ProductQuery.Sort.class, sort)
                .orElseThrow(() -> ApiException.badRequest("request.sort", "Unknown sort order."));
        Care.Placement where = placement == null || placement.isBlank() ? null
                : Care.parse(Care.Placement.class, placement)
                        .filter(p -> p != Care.Placement.BOTH)
                        .orElseThrow(() -> ApiException.badRequest("request.placement", "Placement is indoor or outdoor."));
        return search.search(new ProductQuery(blankToNull(category), blankToNull(collection), blankToNull(q),
                values(Care.Light.class, light, "light"), values(Care.Level.class, care, "care"), where,
                petFriendly, values(Care.Size.class, size, "size"), minPrice, maxPrice, sale, order, page,
                pageSize));
    }

    private static <E extends Enum<E>> List<E> values(Class<E> type, String raw, String name) {
        return split(raw).stream()
                .map(v -> Care.parse(type, v).orElseThrow(
                        () -> ApiException.badRequest("request." + name, "Unknown " + name + ": " + v)))
                .distinct()
                .toList();
    }

    @GetMapping("/products/{slug}")
    ProductDetail product(@PathVariable String slug) {
        return catalog.product(slug);
    }

    @GetMapping("/products/{slug}/related")
    List<ProductCard> related(@PathVariable String slug) {
        return catalog.related(slug, 8);
    }

    private static List<String> split(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(20)
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
