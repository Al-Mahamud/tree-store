"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Heart } from "lucide-react";
import { request } from "@/lib/client";
import type { ProductCard } from "@/lib/types";
import { ProductCardView } from "@/components/shop/ProductCardView";
import { useWishlist } from "./WishlistProvider";

/** Saved plants. Un-hearting one takes it off this page straight away. */
export function Wishlist() {
  const { ids } = useWishlist();
  const [cards, setCards] = useState<ProductCard[] | null>(null);
  useEffect(() => {
    request<ProductCard[]>("GET", "/account/wishlist").then(setCards).catch(() => setCards([]));
  }, []);
  if (!cards) return <div className="h-72 animate-pulse rounded-[2rem] bg-white/60" aria-busy="true" />;
  const shown = cards.filter((c) => ids.includes(c.id));
  if (shown.length === 0) {
    return (
      <div className="grid place-items-center rounded-[2rem] bg-white/60 px-6 py-16 text-center">
        <span className="grid h-16 w-16 place-items-center rounded-full bg-sage text-forest"><Heart size={26} /></span>
        <h2 className="mt-4 font-display text-3xl">Nothing saved yet</h2>
        <p className="mt-1 max-w-xs text-sm text-muted">Tap the heart on any plant to keep it here for later.</p>
        <Link href="/shop" className="mt-6 rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone">Browse plants</Link>
      </div>
    );
  }
  return (
    <section aria-label="Wishlist">
      <p className="mb-4 text-sm text-muted">{shown.length} saved plant{shown.length === 1 ? "" : "s"}</p>
      <div className="grid grid-cols-2 gap-x-4 gap-y-8 md:grid-cols-3">
        {shown.map((p) => <ProductCardView key={p.id} product={p} />)}
      </div>
    </section>
  );
}
