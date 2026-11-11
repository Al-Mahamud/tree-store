"use client";

import { useState } from "react";
import { ApiError, request } from "@/lib/client";
import type { OrderView } from "@/lib/types";
import { OrderDetails, StatusTimeline } from "./OrderDetails";

export function TrackForm({ initialNumber }: { initialNumber: string }) {
  const [number, setNumber] = useState(initialNumber);
  const [phone, setPhone] = useState("");
  const [order, setOrder] = useState<OrderView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  return (
    <>
      <form
        className="mt-8 grid gap-2 rounded-[2rem] bg-mist p-2 sm:grid-cols-[1fr_1fr_auto]"
        onSubmit={async (e) => {
          e.preventDefault();
          setLoading(true);
          setError(null);
          try {
            setOrder(await request<OrderView>("GET", `/orders/track?number=${encodeURIComponent(number)}&phone=${encodeURIComponent(phone)}`));
          } catch (err) {
            setOrder(null);
            setError(err instanceof ApiError && err.status === 404 ? "We couldn't find an order with those details. Check both numbers and try again." : "Something went wrong. Please try again.");
          } finally {
            setLoading(false);
          }
        }}
      >
        <label><span className="sr-only">Order number</span><input required value={number} onChange={(e) => setNumber(e.target.value.toUpperCase())} placeholder="Order number, e.g. SB10001" className="w-full rounded-full bg-white px-5 py-3.5 text-sm outline-none focus:ring-4 focus:ring-lime/40" /></label>
        <label><span className="sr-only">Mobile number</span><input required type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Mobile number, 01XXXXXXXXX" className="w-full rounded-full bg-white px-5 py-3.5 text-sm outline-none focus:ring-4 focus:ring-lime/40" /></label>
        <button type="submit" disabled={loading} className="rounded-full bg-forest px-8 py-3.5 text-sm font-semibold text-bone disabled:opacity-50">{loading ? "Looking…" : "Track"}</button>
      </form>
      {error && <p role="alert" className="mt-4 px-2 text-sm text-sale">{error}</p>}
      {order && (
        <div className="mt-10 animate-rise space-y-6">
          <div className="rounded-[2rem] bg-white/60 p-6 md:p-10"><p className="kicker mb-8 text-center text-moss">Order {order.number}</p><StatusTimeline order={order} /></div>
          <OrderDetails order={order} />
        </div>
      )}
    </>
  );
}
