"use client";

import { ShoppingBag } from "lucide-react";
import { useCart } from "@/components/cart/CartProvider";

export function BagButton() {
  const { cart, setOpen } = useCart();
  const count = cart?.itemCount ?? 0;
  return (
    <button
      type="button"
      onClick={() => setOpen(true)}
      aria-label={`Bag, ${count} ${count === 1 ? "plant" : "plants"}`}
      className="ml-1 inline-flex h-10 items-center gap-2 rounded-full bg-forest pl-3.5 pr-4 text-sm font-semibold text-bone transition hover:bg-forest-deep"
    >
      <ShoppingBag size={17} strokeWidth={1.8} />
      <span key={count} className="animate-rise tabular-nums">{count}</span>
    </button>
  );
}
