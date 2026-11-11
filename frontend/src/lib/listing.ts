/** URL parameters the plant grid understands; anything else in the address is ignored. */
export const FILTER_KEYS = ["q", "light", "care", "placement", "petFriendly", "size", "minPrice", "maxPrice", "sale", "sort"] as const;

export type SearchParams = Record<string, string | string[] | undefined>;

export function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export function productQuery(scope: Record<string, string>, params: SearchParams, pageSize = 24): URLSearchParams {
  const query = new URLSearchParams(scope);
  for (const key of FILTER_KEYS) {
    const value = first(params[key]);
    if (value) query.set(key, value);
  }
  query.set("pageSize", String(pageSize));
  return query;
}

export const SORTS = [
  { value: "recommended", label: "Recommended" },
  { value: "popular", label: "Best selling" },
  { value: "newest", label: "Newest" },
  { value: "price_asc", label: "Price: low to high" },
  { value: "price_desc", label: "Price: high to low" },
] as const;

export const PRICE_BANDS = [
  { label: "Under ৳500", min: undefined, max: 499 },
  { label: "৳500 – ৳1,000", min: 500, max: 1000 },
  { label: "৳1,000 – ৳2,500", min: 1000, max: 2500 },
  { label: "Over ৳2,500", min: 2500, max: undefined },
] as const;
