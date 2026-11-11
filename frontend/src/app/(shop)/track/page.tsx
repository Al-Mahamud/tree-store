import type { Metadata } from "next";
import { TrackForm } from "@/components/order/TrackForm";
import { first, type SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Track your order" };

export default async function TrackPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  return (
    <div className="container-page max-w-5xl py-10 md:py-16">
      <p className="kicker text-moss">Order status</p>
      <h1 className="mt-3 font-display text-5xl md:text-7xl">Where are my <em>plants?</em></h1>
      <p className="mt-3 max-w-lg text-sm text-ink-soft">Enter your order number and the mobile number you ordered with.</p>
      <TrackForm initialNumber={first((await searchParams).number) ?? ""} />
    </div>
  );
}
