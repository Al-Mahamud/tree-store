"use client";

import { useState } from "react";
import { Check, ShoppingBag } from "lucide-react";
import { useCart } from "@/components/cart/CartProvider";
import { ApiError } from "@/lib/client";
import { taka } from "@/lib/money";

export function AddToBag({ variantId, price, soldOut }: { variantId: number | null; price: number | null; soldOut: boolean }) {
  const { add } = useCart();
  const [state, setState] = useState<"idle" | "adding" | "added">("idle");
  const [error, setError] = useState<string | null>(null);
  const onClick = async () => {
    if (variantId === null) return;
    setState("adding");
    setError(null);
    try {
      await add(variantId, 1);
      setState("added");
      setTimeout(() => setState("idle"), 1800);
    } catch (e) {
      setState("idle");
      setError(e instanceof ApiError ? e.message : "Couldn't add to your bag. Please try again.");
    }
  };
  return (
    <div className="flex-1">
      <button type="button" disabled={soldOut || variantId === null || state === "adding"} onClick={onClick} className="flex h-14 w-full items-center justify-center gap-2 rounded-full bg-forest text-sm font-semibold text-bone transition hover:bg-forest-deep disabled:cursor-not-allowed disabled:bg-muted/60">
        {state === "added" ? <Check size={18} /> : <ShoppingBag size={18} strokeWidth={1.8} />}
        {soldOut ? "Sold out in this option" : state === "adding" ? "Adding…" : state === "added" ? "Added to your bag" : `Add to bag${price ? ` · ${taka(price)}` : ""}`}
      </button>
      {error && <p role="alert" className="mt-2 text-center text-xs text-sale">{error}</p>}
    </div>
  );
}
