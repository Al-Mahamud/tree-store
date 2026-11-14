"use client";

import { useState } from "react";
import { ApiError, request } from "@/lib/client";
import type { Reordered } from "@/lib/types";
import { useCart } from "@/components/cart/CartProvider";

/** "Order again": puts an earlier order's plants back in the bag, same size and planter, and opens it. */
export function useReorder() {
  const { refresh, setOpen, notify } = useCart();
  const [busy, setBusy] = useState<string | null>(null);
  const reorder = async (number: string) => {
    setBusy(number);
    try {
      const result = await request<Reordered>("POST", `/account/orders/${encodeURIComponent(number)}/reorder`);
      await refresh();
      if (result.unavailable.length > 0) notify(`Sold out for now: ${result.unavailable.join(", ")}`);
      else if (result.added === 0) notify("Everything from this order is already in your bag.");
      // Only show the bag when something is in it.
      if (result.cart.items.length > 0) setOpen(true);
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Couldn't add these plants. Please try again.");
    } finally {
      setBusy(null);
    }
  };
  return { reorder, busy };
}
