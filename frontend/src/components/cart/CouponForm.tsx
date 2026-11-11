"use client";

import { useState } from "react";
import { Tag, X } from "lucide-react";
import { ApiError } from "@/lib/client";
import type { Cart } from "@/lib/types";
import { useCart } from "./CartProvider";

export function CouponForm({ cart }: { cart: Cart }) {
  const { applyCoupon, removeCoupon } = useCart();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  if (cart.coupon) {
    return (
      <div className="flex items-center justify-between gap-3 rounded-full bg-lime/40 py-1.5 pl-4 pr-1.5 text-sm">
        <span className="flex min-w-0 items-center gap-2"><Tag size={15} className="shrink-0" /><span className="truncate"><strong>{cart.coupon.code}</strong> · {cart.coupon.description}</span></span>
        <button type="button" aria-label="Remove code" onClick={() => removeCoupon()} className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-bone"><X size={14} /></button>
      </div>
    );
  }
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (!code.trim()) return;
        setPending(true);
        setError(null);
        try {
          await applyCoupon(code);
          setCode("");
        } catch (err) {
          setError(err instanceof ApiError ? err.message : "Couldn't apply that code.");
        } finally {
          setPending(false);
        }
      }}
    >
      <label htmlFor="coupon" className="sr-only">Promo code</label>
      <div className="flex items-center rounded-full border border-line bg-white p-1 focus-within:border-forest">
        <input id="coupon" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="Promo code" autoComplete="off" className="min-w-0 flex-1 bg-transparent px-4 text-sm uppercase tracking-wide outline-none placeholder:normal-case placeholder:tracking-normal" />
        <button type="submit" disabled={pending} className="rounded-full bg-forest px-5 py-2 text-sm font-semibold text-bone disabled:opacity-50">Apply</button>
      </div>
      {(error || cart.couponProblem) && <p role="alert" className="mt-2 px-2 text-xs text-sale">{error ?? cart.couponProblem}</p>}
    </form>
  );
}
