"use client";

import { useRef } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import type { ProductCard } from "@/lib/types";
import { ProductCardView } from "./ProductCardView";

export function ProductRail({ products, tone = "light" }: { products: ProductCard[]; tone?: "light" | "dark" }) {
  const track = useRef<HTMLDivElement>(null);
  const scroll = (direction: 1 | -1) => {
    const el = track.current;
    if (el) el.scrollBy({ left: direction * el.clientWidth * 0.8, behavior: "smooth" });
  };
  const button = tone === "dark"
    ? "border-bone/25 text-bone hover:bg-bone hover:text-forest-deep"
    : "border-line bg-bone text-ink hover:bg-forest hover:text-bone";
  return (
    <div>
      <div ref={track} className="no-scrollbar -mx-4 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-px-4 px-4 md:-mx-8 md:gap-6 md:scroll-px-8 md:px-8 xl:-mx-10 xl:scroll-px-10 xl:px-10">
        {products.map((p) => (
          <div key={p.id} className="w-[72%] shrink-0 snap-start sm:w-[40%] lg:w-[27%] xl:w-[23%]">
            <ProductCardView product={p} />
          </div>
        ))}
      </div>
      <div className="mt-6 hidden justify-end gap-2 md:flex">
        <button type="button" aria-label="Previous" onClick={() => scroll(-1)} className={`grid h-11 w-11 place-items-center rounded-full border transition ${button}`}>
          <ArrowLeft size={18} />
        </button>
        <button type="button" aria-label="Next" onClick={() => scroll(1)} className={`grid h-11 w-11 place-items-center rounded-full border transition ${button}`}>
          <ArrowRight size={18} />
        </button>
      </div>
    </div>
  );
}
