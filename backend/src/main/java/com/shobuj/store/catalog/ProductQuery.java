package com.shobuj.store.catalog;

import com.shobuj.store.catalog.Care.Level;
import com.shobuj.store.catalog.Care.Light;
import com.shobuj.store.catalog.Care.Placement;
import com.shobuj.store.catalog.Care.Size;
import java.util.List;

/**
 * Filters for the plant grid. Empty lists and nulls mean "no filter".
 *
 * @param placement INDOOR also matches plants that grow both indoors and out
 * @param minPrice compared with the plant's cheapest option
 */
public record ProductQuery(
        String category,
        String collection,
        String q,
        List<Light> light,
        List<Level> care,
        Placement placement,
        boolean petFriendly,
        List<Size> sizes,
        Integer minPrice,
        Integer maxPrice,
        boolean sale,
        Sort sort,
        int page,
        int pageSize) {

    public enum Sort {
        RECOMMENDED("p.featured desc, p.sold_count desc, p.created_at desc, p.id desc"),
        NEWEST("p.created_at desc, p.id desc"),
        PRICE_ASC("mp.min_price asc, p.id desc"),
        PRICE_DESC("mp.min_price desc, p.id desc"),
        POPULAR("p.sold_count desc, p.id desc");

        final String orderBy;

        Sort(String orderBy) {
            this.orderBy = orderBy;
        }
    }
}
