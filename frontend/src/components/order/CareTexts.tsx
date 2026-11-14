"use client";

import { useState } from "react";
import { Leaf } from "lucide-react";
import type { OrderView } from "@/lib/types";
import { ApiError, request } from "@/lib/client";

/**
 * The care-texts switch on an order: for whoever has the order's link (`token`) or the signed-in
 * customer who placed it.
 */
export function CareTexts({ order, token }: { order: OrderView; token?: string }) {
  const [on, setOn] = useState(order.careTexts);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (order.status === "CANCELLED" || order.status === "PENDING_PAYMENT") return null;

  const toggle = async () => {
    setBusy(true);
    setError(null);
    try {
      const query = token ? `?token=${encodeURIComponent(token)}` : "";
      const r = await request<{ on: boolean }>("PUT", `/orders/${encodeURIComponent(order.number)}/care-texts${query}`, { on: !on });
      setOn(r.on);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn't change that. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  const delivered = order.status === "DELIVERED";
  return (
    <div className="rounded-[2rem] bg-mist p-5 text-sm md:p-7">
      <div className="flex items-start justify-between gap-4">
        <h2 className="flex items-center gap-2 font-display text-2xl"><Leaf size={20} className="text-moss" /> Care texts</h2>
        <button
          type="button"
          role="switch"
          aria-checked={on}
          aria-label="Care texts"
          disabled={busy}
          onClick={toggle}
          className={`relative mt-1 h-7 w-12 shrink-0 rounded-full transition disabled:opacity-60 ${on ? "bg-forest" : "bg-line"}`}
        >
          <span className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow transition-all ${on ? "left-6" : "left-1"}`} />
        </button>
      </div>
      <p className="mt-2 text-ink-soft">
        {on
          ? delivered
            ? "We'll text a few tips as your plants settle in: day 2, day 14, and before the guarantee ends."
            : "After delivery we'll text three short tips: settling in (day 2), watering (day 14) and a reminder before the guarantee ends (day 25)."
          : "Off. We'll only text you about this order's delivery."}
      </p>
      {error && <p role="alert" className="mt-2 text-xs text-sale">{error}</p>}
    </div>
  );
}
