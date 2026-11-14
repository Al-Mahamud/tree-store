import "server-only";
import { notFound } from "next/navigation";
import type { CategoryNode, Collection, DeliveryFees, Home, OrderView, ProductCard, ProductDetail, ProductPage, ReviewPage, Sitemap } from "./types";

const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

/** Catalog reads from server components, cached briefly. */
async function get<T>(path: string, revalidate = 30): Promise<T> {
  const response = await fetch(`${backend}/api/v1${path}`, { next: { revalidate } });
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error(`GET ${path} failed with ${response.status}`);
  return (await response.json()) as T;
}

export const api = {
  home: () => get<Home>("/home"),
  categories: () => get<CategoryNode[]>("/categories", 300),
  category: (slug: string) => get<CategoryNode>(`/categories/${encodeURIComponent(slug)}`),
  collections: () => get<Collection[]>("/collections", 300),
  collection: (slug: string) => get<Collection>(`/collections/${encodeURIComponent(slug)}`),
  products: (query: URLSearchParams) => get<ProductPage>(`/products?${query}`),
  product: (slug: string) => get<ProductDetail>(`/products/${encodeURIComponent(slug)}`),
  delivery: () => get<DeliveryFees>("/delivery", 60),
  related: (slug: string) => get<ProductCard[]>(`/products/${encodeURIComponent(slug)}/related`),
  /** The first page of reviews, rendered with the product page (for shoppers and search engines). */
  reviews: (slug: string) => get<ReviewPage>(`/products/${encodeURIComponent(slug)}/reviews`, 60),
  sitemap: () => get<Sitemap>("/sitemap", 3600),
  /** Never cached: order status changes. */
  order: async (number: string, token: string): Promise<OrderView> => {
    const response = await fetch(
      `${backend}/api/v1/orders/${encodeURIComponent(number)}?token=${encodeURIComponent(token)}`,
      { cache: "no-store" },
    );
    if (response.status === 404) notFound();
    if (!response.ok) throw new Error(`Order lookup failed with ${response.status}`);
    return (await response.json()) as OrderView;
  },
};
