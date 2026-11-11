"use client";

import { useState } from "react";
import type { ProductCard, ProductPage } from "@/lib/types";
import { ProductGrid } from "./ProductGrid";

/** Shows the first page from the server and fetches further pages in the browser. */
export function LoadMoreGrid({ initial, query }: { initial: ProductPage; query: string }) {
  const [items, setItems] = useState<ProductCard[]>(initial.items);
  const [page, setPage] = useState(initial.page);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);

  const loadMore = async () => {
    setLoading(true);
    setFailed(false);
    try {
      const params = new URLSearchParams(query);
      params.set("page", String(page + 1));
      const response = await fetch(`/api/v1/products?${params}`);
      if (!response.ok) throw new Error(String(response.status));
      const next = (await response.json()) as ProductPage;
      setItems((current) => [...current, ...next.items.filter((n) => !current.some((c) => c.id === n.id))]);
      setPage(next.page);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  };

  const shown = items.length;
  return (
    <>
      <ProductGrid products={items} />
      {shown < initial.total && (
        <div className="mt-16 flex flex-col items-center gap-4">
          <p className="text-xs text-muted">Showing {shown} of {initial.total}</p>
          <div className="h-1.5 w-48 overflow-hidden rounded-full bg-mist">
            <div className="h-full rounded-full bg-moss transition-all" style={{ width: `${(shown / initial.total) * 100}%` }} />
          </div>
          <button
            type="button"
            onClick={loadMore}
            disabled={loading}
            className="mt-2 rounded-full bg-forest px-10 py-3.5 text-sm font-semibold text-bone transition hover:bg-forest-deep disabled:opacity-50"
          >
            {loading ? "Loading…" : "Show more plants"}
          </button>
          {failed && <p role="alert" className="text-sm text-sale">Couldn&rsquo;t load more. Please try again.</p>}
        </div>
      )}
    </>
  );
}
