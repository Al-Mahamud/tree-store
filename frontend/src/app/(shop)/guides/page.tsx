import type { Metadata } from "next";
import { guides } from "@/config/guides";
import { GuideCard } from "@/components/guide/GuideCard";

export const metadata: Metadata = {
  title: "Planting & care guides",
  description: "Practical guides to planting fruit trees, rooftop gardens, indoor plants, watering and bonsai in Bangladesh's climate.",
  alternates: { canonical: "/guides" },
};

export default function GuidesPage() {
  return (
    <div className="container-page pt-10 md:pt-16">
      <p className="kicker text-moss">From the potting shed</p>
      <h1 className="mt-3 max-w-3xl font-display text-5xl leading-[0.98] md:text-7xl">Planting &amp; care <em>guides</em></h1>
      <p className="mt-5 max-w-xl text-ink-soft">Short, practical advice from our nursery team, written for Bangladesh&rsquo;s heat, monsoon and rooftops.</p>
      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 md:mt-14">
        {guides.map((g) => <GuideCard key={g.slug} guide={g} />)}
      </div>
    </div>
  );
}
