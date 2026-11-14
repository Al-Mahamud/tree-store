"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, RotateCcw, Sprout } from "lucide-react";
import { request } from "@/lib/client";
import { taka } from "@/lib/money";
import type { OrderHistory, OrderSummary } from "@/lib/types";
import { StatusPill } from "./StatusPill";
import { useReorder } from "./useReorder";

/* eslint-disable @next/next/no-img-element */
export function OrderList() {
  const [orders, setOrders] = useState<OrderSummary[] | null>(null);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const { reorder, busy } = useReorder();

  useEffect(() => {
    let live = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- loading a page of orders.
    setLoading(true);
    request<OrderHistory>("GET", `/account/orders?page=${page}`)
      .then((h) => {
        if (!live) return;
        setOrders((o) => (page === 0 ? h.items : [...(o ?? []), ...h.items]));
        setTotal(h.total);
      })
      .catch(() => live && setOrders((o) => o ?? []))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [page]);

  if (!orders) return <div className="h-64 animate-pulse rounded-[2rem] bg-white/60" aria-busy="true" />;
  if (orders.length === 0) {
    return (
      <div className="grid place-items-center rounded-[2rem] bg-white/60 px-6 py-16 text-center">
        <span className="grid h-16 w-16 place-items-center rounded-full bg-sage text-forest"><Sprout size={28} /></span>
        <h2 className="mt-4 font-display text-3xl">No orders yet</h2>
        <p className="mt-1 max-w-xs text-sm text-muted">Your plants&rsquo; journeys will show up here.</p>
        <Link href="/shop" className="mt-6 rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone">Find a plant</Link>
      </div>
    );
  }
  return (
    <section aria-label="Your orders">
      <ul className="space-y-3">
        {orders.map((o) => (
          <li key={o.number} className="rounded-[2rem] bg-white/60 p-5 md:p-6">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <Link href={`/account/orders/${o.number}`} className="font-display text-2xl hover:text-moss">{o.number}</Link>
              <StatusPill status={o.status} />
              <span className="ml-auto text-xs text-muted">{new Date(o.placedAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}</span>
            </div>
            <div className="mt-4 flex flex-wrap items-center gap-4">
              <div className="flex -space-x-3">
                {o.images.map((src, i) => <span key={i} className="h-14 w-12 overflow-hidden rounded-xl border-2 border-bone bg-mist"><img src={src} alt="" className="h-full w-full object-cover" /></span>)}
                {o.lineCount > o.images.length && <span className="grid h-14 w-12 place-items-center rounded-xl border-2 border-bone bg-sage text-xs font-semibold">+{o.lineCount - o.images.length}</span>}
              </div>
              <p className="min-w-0 flex-1 text-sm text-muted">{o.itemCount} plant{o.itemCount === 1 ? "" : "s"} · <strong className="text-ink tabular-nums">{taka(o.total)}</strong></p>
              <div className="flex w-full gap-2 sm:w-auto">
                <button type="button" onClick={() => reorder(o.number)} disabled={busy !== null} className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-full bg-forest px-5 py-2.5 text-sm font-semibold text-bone disabled:opacity-60 sm:flex-none">
                  <RotateCcw size={15} /> {busy === o.number ? "Adding…" : "Order again"}
                </button>
                <Link href={`/account/orders/${o.number}`} className="inline-flex flex-1 items-center justify-center gap-0.5 rounded-full border border-line px-5 py-2.5 text-sm font-semibold hover:border-forest sm:flex-none">Details <ChevronRight size={15} /></Link>
              </div>
            </div>
          </li>
        ))}
      </ul>
      {orders.length < total && (
        <div className="mt-5 text-center">
          <button type="button" onClick={() => setPage((p) => p + 1)} disabled={loading} className="rounded-full border border-forest px-6 py-2.5 text-sm font-semibold disabled:opacity-50">{loading ? "Loading…" : "Show older orders"}</button>
        </div>
      )}
    </section>
  );
}
