import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CircleCheck, Sprout, XCircle } from "lucide-react";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { site } from "@/config/site";
import { taka } from "@/lib/money";
import { OrderDetails, StatusTimeline } from "@/components/order/OrderDetails";
import { AccountNudge } from "@/components/account/AccountNudge";
import { PaymentPanel } from "@/components/payment/PaymentPanel";

export const metadata: Metadata = { title: "Your order", robots: { index: false } };

const outcomes = ["success", "fail", "cancel"] as const;

export default async function OrderPage({ params, searchParams }: { params: Promise<{ number: string }>; searchParams: Promise<SearchParams> }) {
  const { number } = await params;
  const query = await searchParams;
  const token = first(query.t);
  if (!token) notFound();
  const order = await api.order(number, token);
  const payment = outcomes.find((o) => o === first(query.payment));
  const paid = order.paymentStatus === "PAID";
  const live = order.status !== "PENDING_PAYMENT" && order.status !== "CANCELLED";

  return (
    <div className="container-page max-w-5xl py-10 md:py-16">
      {order.status === "PENDING_PAYMENT" ? (
        <PaymentPanel order={order} token={token} outcome={payment} />
      ) : order.status === "CANCELLED" ? (
        <div className="animate-rise rounded-[2.5rem] bg-white/60 px-6 py-12 text-center md:py-16">
          <XCircle className="mx-auto text-sale" size={40} />
          <p className="kicker mt-5 text-muted">Order {order.number}</p>
          <h1 className="mt-3 font-display text-5xl leading-none">This order was cancelled.</h1>
          <p className="mx-auto mt-4 max-w-lg text-ink-soft">
            {paid ? `We received ${taka(order.total)} after the order closed. We'll refund it to the same account — no need to do anything.`
              : "It wasn't paid in time, so nothing was charged and the plants went back to the nursery."}
          </p>
          <Link href="/cart" className="mt-6 inline-block rounded-full bg-forest px-8 py-3.5 text-sm font-semibold text-bone">Back to your bag</Link>
        </div>
      ) : (
        <div className="animate-rise rounded-[2.5rem] bg-sage px-6 py-12 text-center md:py-16">
          <span className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-forest text-lime"><Sprout size={30} /></span>
          <p className="kicker mt-6 text-forest">Order {order.number}</p>
          <h1 className="mt-3 font-display text-5xl leading-none md:text-7xl">Thank you, <em>{order.name.split(" ")[0]}.</em></h1>
          {paid && <p className="mt-5 inline-flex items-center gap-2 rounded-full bg-forest px-5 py-2 text-sm font-semibold text-bone"><CircleCheck size={16} className="text-lime" /> Paid {taka(order.total)}</p>}
          <p className="mx-auto mt-5 max-w-lg text-ink-soft">
            Your plants are being picked at the nursery. We&rsquo;ll call <strong className="font-semibold text-ink">{order.phone}</strong> to confirm, and a care guide comes with every plant.
          </p>
        </div>
      )}
      {live && <div className="mt-6 rounded-[2rem] bg-white/60 p-6 md:p-10"><StatusTimeline order={order} /></div>}
      <div className="mt-6"><OrderDetails order={order} care={{ token }} /></div>
      {order.status !== "PENDING_PAYMENT" && <AccountNudge />}
      <div className="mt-10 flex flex-wrap justify-center gap-3">
        <Link href="/shop" className="rounded-full bg-forest px-8 py-3.5 text-sm font-semibold text-bone">Keep browsing</Link>
        <Link href={`/track?number=${order.number}`} className="rounded-full border border-forest px-8 py-3.5 text-sm font-semibold">Track this order</Link>
      </div>
      <p className="mt-8 text-center text-xs text-muted">Questions about planting? WhatsApp our plant doctor on {site.whatsapp}.</p>
    </div>
  );
}
