import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Sprout } from "lucide-react";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { site } from "@/config/site";
import { OrderDetails, StatusTimeline } from "@/components/order/OrderDetails";
import { AccountNudge } from "@/components/account/AccountNudge";

export const metadata: Metadata = { title: "Thank you", robots: { index: false } };

export default async function OrderPage({ params, searchParams }: { params: Promise<{ number: string }>; searchParams: Promise<SearchParams> }) {
  const { number } = await params;
  const token = first((await searchParams).t);
  if (!token) notFound();
  const order = await api.order(number, token);
  return (
    <div className="container-page max-w-5xl py-10 md:py-16">
      <div className="animate-rise rounded-[2.5rem] bg-sage px-6 py-12 text-center md:py-16">
        <span className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-forest text-lime"><Sprout size={30} /></span>
        <p className="kicker mt-6 text-forest">Order {order.number}</p>
        <h1 className="mt-3 font-display text-5xl leading-none md:text-7xl">Thank you, <em>{order.name.split(" ")[0]}.</em></h1>
        <p className="mx-auto mt-5 max-w-lg text-ink-soft">
          Your plants are being picked at the nursery. We&rsquo;ll call <strong className="font-semibold text-ink">{order.phone}</strong> to confirm, and a care guide comes with every plant.
        </p>
      </div>
      <div className="mt-6 rounded-[2rem] bg-white/60 p-6 md:p-10"><StatusTimeline order={order} /></div>
      <div className="mt-6"><OrderDetails order={order} /></div>
      <AccountNudge />
      <div className="mt-10 flex flex-wrap justify-center gap-3">
        <Link href="/shop" className="rounded-full bg-forest px-8 py-3.5 text-sm font-semibold text-bone">Keep browsing</Link>
        <Link href={`/track?number=${order.number}`} className="rounded-full border border-forest px-8 py-3.5 text-sm font-semibold">Track this order</Link>
      </div>
      <p className="mt-8 text-center text-xs text-muted">Questions about planting? WhatsApp our plant doctor on {site.whatsapp}.</p>
    </div>
  );
}
