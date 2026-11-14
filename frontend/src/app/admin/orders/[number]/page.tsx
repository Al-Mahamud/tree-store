"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Check, MessageSquarePlus, Phone, Printer, Truck, Undo2, X } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { AdminOrderDetail, OrderStatus } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { dateTime } from "@/lib/format";
import { StatusPill } from "@/components/account/StatusPill";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PaymentBadge, ZoneBadge, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const action: Partial<Record<OrderStatus, string>> = {
  CONFIRMED: "Confirm order",
  SHIPPED: "Send out",
  DELIVERED: "Mark delivered",
};

const eventLabel: Record<string, string> = {
  PENDING_PAYMENT: "Awaiting payment",
  PLACED: "Placed",
  CONFIRMED: "Confirmed",
  SHIPPED: "Sent out",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

/* eslint-disable @next/next/no-img-element */
export default function AdminOrderPage({ params }: { params: Promise<{ number: string }> }) {
  const { number } = use(params);
  const { notify } = useAdmin();
  const [data, setData] = useState<AdminOrderDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [shipping, setShipping] = useState(false);
  const [note, setNote] = useState("");
  const [restock, setRestock] = useState(true);
  const [reference, setReference] = useState("");
  const [courier, setCourier] = useState("");
  const [tracking, setTracking] = useState("");

  const load = useCallback(() => {
    request<AdminOrderDetail>("GET", `/admin/orders/${number}`)
      .then(setData)
      .catch((e) => setError(e instanceof ApiError && e.status === 404 ? "There's no order with that number." : "Couldn't load the order."));
  }, [number]);
  useEffect(load, [load]);

  const send = async (path: string, body: unknown, done: string) => {
    setBusy(true);
    try {
      setData(await request<AdminOrderDetail>("POST", `/admin/orders/${number}${path}`, body));
      notify(done);
      return true;
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Something went wrong.");
      return false;
    } finally {
      setBusy(false);
    }
  };

  if (error) return <p className="rounded-3xl bg-white p-6 font-semibold text-sale">{error}</p>;
  if (!data) return <Loading />;
  const o = data.order;
  const forward = data.next.filter((s) => s !== "CANCELLED");
  const plants = o.items.reduce((n, i) => n + i.quantity, 0);

  return (
    <div className="space-y-4">
      <Link href="/admin/orders" className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink print:hidden"><ArrowLeft size={15} /> Orders</Link>
      <div className="flex flex-wrap items-center gap-3 rounded-3xl border border-line bg-white p-4 md:p-5">
        <div>
          <p className="text-xs font-semibold text-muted">Placed {dateTime(o.placedAt)}</p>
          <h1 className="font-display text-4xl leading-tight">{o.number}</h1>
        </div>
        <StatusPill status={o.status} />
        <PaymentBadge method={o.paymentMethod} status={o.paymentStatus} />
        <div className="ml-auto flex flex-wrap gap-2 print:hidden">
          <button type="button" onClick={() => window.print()} className={btn.ghost}><Printer size={15} /> Packing slip</button>
          {data.next.includes("CANCELLED") && <button type="button" onClick={() => setCancelling(true)} className={btn.ghost}><X size={15} /> Cancel</button>}
          {forward.map((s) => (
            <button
              key={s}
              type="button"
              disabled={busy}
              onClick={() => (s === "SHIPPED" ? setShipping(true) : void send("/status", { status: s }, `${o.number}: ${(eventLabel[s] ?? s).toLowerCase()}`))}
              className={btn.primary}
            >
              {s === "SHIPPED" ? <Truck size={15} /> : <Check size={15} />} {action[s]}
              {s === "DELIVERED" && o.paymentMethod === "COD" && o.paymentStatus === "UNPAID" ? ` · collect ${taka(o.total)}` : ""}
            </button>
          ))}
        </div>
      </div>

      {data.refundDue && (
        <Card title="Refund due" className="border-sale/40 bg-sale/5">
          <p className="text-sm">This order was paid {taka(o.total)} online and then cancelled. Refund it in the SSLCommerz merchant panel, then record the refund reference here.</p>
          <form className="mt-3 flex flex-wrap gap-2" onSubmit={async (e) => { e.preventDefault(); if (await send("/refund", { reference }, "Refund recorded")) setReference(""); }}>
            <input value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Refund reference" className={`${input} max-w-xs`} />
            <button type="submit" disabled={busy || !reference.trim()} className={btn.danger}><Undo2 size={15} /> Mark refunded</button>
          </form>
        </Card>
      )}
      {data.refundedAt && <p className="rounded-3xl bg-mist px-4 py-3 text-sm">Refunded {dateTime(data.refundedAt)} · reference <strong>{data.refundReference}</strong></p>}

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title={`Plants (${plants})`} className="lg:col-span-2">
          <ul className="divide-y divide-line">
            {o.items.map((i, index) => (
              <li key={`${i.slug}-${i.size}-${i.planter}`} className="flex items-center gap-3 py-2.5">
                <span className="h-14 w-14 shrink-0 overflow-hidden rounded-2xl bg-mist">{i.image && <img src={i.image} alt="" className="h-full w-full object-cover" />}</span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold">{i.name}</span>
                  <span className="block text-xs text-muted">{i.size} ({i.height}) · {i.planter} · {taka(i.unitPrice)} each</span>
                  {data.skus[index] && <span className="font-mono text-[0.7rem] text-muted">{data.skus[index]}</span>}
                </span>
                <span className="rounded-full bg-mist px-3 py-1 text-sm font-semibold tabular-nums">×{i.quantity}</span>
                <span className="w-20 text-right text-sm font-semibold tabular-nums">{taka(i.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-2 space-y-1 border-t border-line pt-3 text-sm">
            <div className="flex justify-between"><dt className="text-muted">Plants</dt><dd className="tabular-nums">{taka(o.subtotal)}</dd></div>
            {o.discount > 0 && <div className="flex justify-between text-ok"><dt>Coupon {o.couponCode}</dt><dd className="tabular-nums">−{taka(o.discount)}</dd></div>}
            <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{o.shippingFee === 0 ? "Free" : taka(o.shippingFee)}</dd></div>
            <div className="flex justify-between text-base font-semibold"><dt>Total</dt><dd className="tabular-nums">{taka(o.total)}</dd></div>
          </dl>
        </Card>
        <div className="space-y-4">
          <Card title="Delivery" action={<ZoneBadge zone={o.zone} />}>
            <p className="font-semibold">{o.name}</p>
            <a href={`tel:${o.phone}`} className="inline-flex items-center gap-1 text-sm font-semibold text-moss"><Phone size={13} /> {o.phone}</a>
            {o.email && <p className="text-sm text-ink-soft">{o.email}</p>}
            <p className="mt-1 text-sm text-ink-soft">{o.address}, {o.area}, {o.city}</p>
            {o.note && <p className="mt-2 rounded-2xl bg-lime/30 px-3 py-2 text-sm">“{o.note}”</p>}
            {o.courier && <p className="mt-3 text-sm"><span className="text-muted">Courier:</span> <strong>{o.courier}</strong>{o.trackingCode && <> · <span className="font-mono">{o.trackingCode}</span></>}</p>}
            <p className="mt-3 text-xs text-muted">{data.customerId ? "Has an account" : "Guest"} · {data.customerOrders} order{data.customerOrders === 1 ? "" : "s"} with this number or account</p>
          </Card>
          {data.attempts.length > 0 && (
            <Card title="Online payment">
              <ul className="space-y-2 text-sm">
                {data.attempts.map((a) => (
                  <li key={a.tranId} className="flex items-center justify-between gap-2">
                    <span><span className="block font-mono text-xs">{a.tranId}</span><span className="text-xs text-muted">{dateTime(a.at)}{a.cardType ? ` · ${a.cardType}` : ""}</span></span>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${a.status === "PAID" ? "bg-ok/15 text-ok" : "bg-mist text-ink-soft"}`}>{a.status.toLowerCase()}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
          <Card title="History">
            <ol className="space-y-3 border-l-2 border-line pl-4">
              {[...o.events].reverse().map((e, i) => (
                <li key={i} className="relative text-sm">
                  <span className="absolute -left-[1.4rem] top-1 h-2.5 w-2.5 rounded-full bg-moss" />
                  <span className="font-semibold">{eventLabel[e.status] ?? e.status}</span>
                  <span className="block text-xs text-muted">{dateTime(e.at)}</span>
                  {e.note && <span className="block text-ink-soft">{e.note}</span>}
                </li>
              ))}
            </ol>
            <form className="mt-4 flex gap-2 print:hidden" onSubmit={async (e) => { e.preventDefault(); if (await send("/notes", { note }, "Note added")) setNote(""); }}>
              <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Add a note for the team" className={input} />
              <button type="submit" aria-label="Add note" disabled={busy || !note.trim()} className={btn.icon}><MessageSquarePlus size={16} /></button>
            </form>
          </Card>
        </div>
      </div>

      <Modal open={shipping} onClose={() => setShipping(false)} title={`Send out ${o.number}`}>
        <form
          className="space-y-3 text-sm"
          onSubmit={async (e) => {
            e.preventDefault();
            if (await send("/status", { status: "SHIPPED", courier, trackingCode: tracking }, `${o.number} is on the way`)) {
              setShipping(false);
              setCourier("");
              setTracking("");
            }
          }}
        >
          <p className="text-ink-soft">{o.zone === "DHAKA" ? "Inside Dhaka our own van usually takes it; leave the courier empty." : "Outside Dhaka: note the courier and its tracking number so the customer can follow the parcel."}</p>
          <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Courier</span><input value={courier} onChange={(e) => setCourier(e.target.value)} list="couriers" placeholder={o.zone === "DHAKA" ? "Our van" : "e.g. Sundarban Courier"} className={input} /></label>
          <datalist id="couriers">
            {["Sundarban Courier", "SA Paribahan", "Pathao Courier", "Steadfast", "RedX"].map((c) => <option key={c} value={c} />)}
          </datalist>
          <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Tracking number</span><input value={tracking} onChange={(e) => setTracking(e.target.value)} className={input} /></label>
          <div className="flex gap-2 pt-2">
            <button type="submit" disabled={busy} className={btn.primary}><Truck size={15} /> Send out</button>
            <button type="button" onClick={() => setShipping(false)} className={btn.ghost}>Not yet</button>
          </div>
        </form>
      </Modal>

      <Modal open={cancelling} onClose={() => setCancelling(false)} title={`Cancel ${o.number}?`}>
        <div className="space-y-3 text-sm">
          {o.paymentStatus === "PAID" && <p className="rounded-2xl bg-sale/10 p-3 font-semibold text-sale">This order is paid. It will show as a refund due.</p>}
          <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Reason (the team sees this)</span><input value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Customer asked to cancel" className={input} /></label>
          <label className="flex items-center gap-2 font-semibold"><input type="checkbox" checked={restock} onChange={(e) => setRestock(e.target.checked)} className="h-4 w-4 accent-forest" /> Put the plants back in stock</label>
          <p className="text-xs text-muted">Untick if the plants can&rsquo;t be sold again, e.g. a parcel that came back wilted.</p>
          <div className="flex gap-2 pt-2">
            <button type="button" disabled={busy} onClick={async () => { if (await send("/status", { status: "CANCELLED", note, restock }, `${o.number} cancelled`)) { setCancelling(false); setNote(""); } }} className={btn.danger}>Cancel order</button>
            <button type="button" onClick={() => setCancelling(false)} className={btn.ghost}>Keep it</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
