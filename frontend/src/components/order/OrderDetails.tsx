import Link from "next/link";
import { Check } from "lucide-react";
import type { OrderView } from "@/lib/types";
import { taka } from "@/lib/money";

const steps = [
  { status: "PLACED", label: "Placed" },
  { status: "CONFIRMED", label: "Confirmed" },
  { status: "SHIPPED", label: "On its way" },
  { status: "DELIVERED", label: "Planted!" },
] as const;

const when = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit", timeZone: "Asia/Dhaka" });

export function StatusTimeline({ order }: { order: OrderView }) {
  if (order.status === "CANCELLED") return <p className="rounded-2xl bg-sale/10 px-4 py-3 text-sm text-sale">This order was cancelled.</p>;
  if (order.status === "PENDING_PAYMENT") return <p className="rounded-2xl bg-clay/10 px-4 py-3 text-sm">Waiting for your payment to complete.</p>;
  const reached = steps.findIndex((s) => s.status === order.status);
  return (
    <ol className="grid grid-cols-4">
      {steps.map((step, i) => {
        const done = i <= reached;
        const event = order.events.find((e) => e.status === step.status);
        return (
          <li key={step.status} className="relative flex flex-col items-center text-center">
            {i > 0 && <span className={`absolute right-1/2 top-4 h-1 w-full rounded-full ${i <= reached ? "bg-moss" : "bg-mist"}`} aria-hidden />}
            <span className={`relative grid h-9 w-9 place-items-center rounded-full text-xs font-semibold ${done ? "bg-forest text-lime" : "bg-mist text-muted"}`}>{done ? <Check size={15} strokeWidth={2.5} /> : i + 1}</span>
            <span className={`mt-2 text-xs font-semibold ${done ? "" : "text-muted"}`}>{step.label}</span>
            {event && <span className="text-[0.6875rem] text-muted">{when.format(new Date(event.at))}</span>}
          </li>
        );
      })}
    </ol>
  );
}

/* eslint-disable @next/next/no-img-element */
export function OrderDetails({ order }: { order: OrderView }) {
  return (
    <div className="grid gap-6 md:grid-cols-5">
      <div className="rounded-[2rem] bg-white/50 p-5 md:col-span-3 md:p-7">
        <h2 className="font-display text-2xl">Plants</h2>
        <ul className="mt-4 space-y-3">
          {order.items.map((item, i) => (
            <li key={i} className="flex gap-4">
              <Link href={`/p/${item.slug}`} className="w-16 shrink-0 overflow-hidden rounded-xl bg-mist"><div className="aspect-[4/5]">{item.image && <img src={item.image} alt="" className="h-full w-full object-cover" />}</div></Link>
              <div className="flex-1 text-sm"><p className="font-semibold">{item.name}</p><p className="text-xs text-muted">{item.size}, {item.height} · {item.planter} · ×{item.quantity}</p></div>
              <p className="text-sm font-semibold tabular-nums">{taka(item.lineTotal)}</p>
            </li>
          ))}
        </ul>
        <dl className="mt-5 space-y-1.5 border-t border-line pt-4 text-sm">
          <div className="flex justify-between"><dt className="text-muted">Plants</dt><dd className="tabular-nums">{taka(order.subtotal)}</dd></div>
          {order.discount > 0 && <div className="flex justify-between text-ok"><dt>Discount ({order.couponCode})</dt><dd className="tabular-nums">−{taka(order.discount)}</dd></div>}
          <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{order.shippingFee === 0 ? "Free" : taka(order.shippingFee)}</dd></div>
          <div className="flex justify-between pt-2 text-base font-semibold"><dt>Total</dt><dd className="tabular-nums">{taka(order.total)}</dd></div>
        </dl>
      </div>
      <div className="space-y-4 md:col-span-2">
        <div className="rounded-[2rem] bg-mist p-5 text-sm md:p-7">
          <h2 className="font-display text-2xl">Delivering to</h2>
          <p className="mt-3 font-semibold">{order.name}</p>
          <p className="text-ink-soft">{order.address}<br />{order.area}, {order.city}<br />{order.phone}</p>
          {order.note && <p className="mt-2 text-xs italic text-muted">“{order.note}”</p>}
        </div>
        <div className="rounded-[2rem] bg-mist p-5 text-sm md:p-7">
          <h2 className="font-display text-2xl">Payment</h2>
          <p className="mt-3">{order.paymentStatus === "PAID" ? (order.status === "CANCELLED" ? "Paid online — refund on its way" : "Paid online") : order.paymentStatus === "REFUNDED" ? "Refunded" : order.paymentMethod === "COD" ? (order.status === "CANCELLED" ? "Nothing to pay" : `Cash on delivery — please keep ${taka(order.total)} ready.`) : order.status === "CANCELLED" ? "Not paid" : "Online, waiting for payment"}</p>
        </div>
      </div>
    </div>
  );
}
