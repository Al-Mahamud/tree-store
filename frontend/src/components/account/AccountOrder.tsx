"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, RotateCcw } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { OrderView } from "@/lib/types";
import { OrderDetails, StatusTimeline } from "@/components/order/OrderDetails";
import { PaymentPanel } from "@/components/payment/PaymentPanel";
import { StatusPill } from "./StatusPill";
import { useReorder } from "./useReorder";

export function AccountOrder({ number }: { number: string }) {
  const [order, setOrder] = useState<OrderView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { reorder, busy } = useReorder();
  const load = useCallback(() => {
    request<OrderView>("GET", `/account/orders/${encodeURIComponent(number)}`)
      .then(setOrder)
      .catch((e) => setError(e instanceof ApiError && e.status === 404 ? "We couldn't find that order in your account." : "We couldn't load this order. Please refresh."));
  }, [number]);
  useEffect(load, [load]);
  return (
    <div className="space-y-4">
      <Link href="/account" className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink"><ArrowLeft size={15} /> All orders</Link>
      {error && <p className="rounded-[2rem] bg-white/60 p-6 text-sm text-sale">{error}</p>}
      {!order && !error && <div className="h-72 animate-pulse rounded-[2rem] bg-white/60" aria-busy="true" />}
      {order && (
        <>
          <div className="flex flex-wrap items-center gap-3 rounded-[2rem] bg-white/60 p-5 md:p-6">
            <div><p className="kicker text-muted">Order</p><h2 className="font-display text-4xl">{order.number}</h2></div>
            <StatusPill status={order.status} />
            <button type="button" onClick={() => reorder(order.number)} disabled={busy !== null} className="ml-auto inline-flex items-center gap-1.5 rounded-full bg-forest px-6 py-3 text-sm font-semibold text-bone disabled:opacity-60">
              <RotateCcw size={15} /> {busy ? "Adding…" : "Order again"}
            </button>
          </div>
          {order.status === "PENDING_PAYMENT" ? (
            <PaymentPanel order={order} onChanged={load} />
          ) : (
            <div className="rounded-[2rem] bg-white/60 p-6 md:p-8"><StatusTimeline order={order} /></div>
          )}
          <OrderDetails order={order} />
        </>
      )}
    </div>
  );
}
