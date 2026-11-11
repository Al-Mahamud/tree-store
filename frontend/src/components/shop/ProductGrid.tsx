import type { ProductCard } from "@/lib/types";
import { ProductCardView } from "./ProductCardView";

export function ProductGrid({ products, eager = 4 }: { products: ProductCard[]; eager?: number }) {
  return (
    <div className="grid grid-cols-2 gap-x-3 gap-y-10 md:grid-cols-3 md:gap-x-6 xl:grid-cols-4">
      {products.map((p, i) => (
        <ProductCardView key={p.id} product={p} priority={i < eager} />
      ))}
    </div>
  );
}
