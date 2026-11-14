import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, Check, Clock, Lightbulb } from "lucide-react";
import { guides } from "@/config/guides";
import { site } from "@/config/site";
import { api } from "@/lib/api";
import { JsonLd, absolute, breadcrumbs } from "@/lib/jsonld";
import { GuideCard, GuideIcon, guideTone } from "@/components/guide/GuideCard";
import { ProductRail } from "@/components/shop/ProductRail";
import { SectionHeading } from "@/components/shop/SectionHeading";

type Props = { params: Promise<{ slug: string }> };

export function generateStaticParams() {
  return guides.map((g) => ({ slug: g.slug }));
}

export const dynamicParams = false;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const guide = guides.find((g) => g.slug === slug);
  if (!guide) return {};
  return {
    title: guide.title,
    description: guide.summary,
    alternates: { canonical: `/guides/${guide.slug}` },
    openGraph: { title: guide.title, description: guide.summary, type: "article" },
  };
}

export default async function GuidePage({ params }: Props) {
  const { slug } = await params;
  const guide = guides.find((g) => g.slug === slug);
  if (!guide) notFound();
  const query = new URLSearchParams(guide.plants);
  query.set("pageSize", "8");
  const plants = await api.products(query).then((p) => p.items).catch(() => []);
  const more = guides.filter((g) => g.slug !== slug).slice(0, 3);
  const shopHref = guide.plants.startsWith("category=") ? `/c/${guide.plants.slice(9)}`
    : guide.plants.startsWith("collection=") ? `/collections/${guide.plants.slice(11)}` : `/shop?${guide.plants}`;

  return (
    <>
      <article className="container-page pt-6 md:pt-10">
        <Link href="/guides" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft size={15} /> All guides</Link>
        <header className={`mt-5 grid gap-6 overflow-hidden rounded-[2rem] p-7 md:grid-cols-[1fr_auto] md:items-end md:p-12 ${guideTone(guide)}`}>
          <div>
            <p className="inline-flex items-center gap-1.5 rounded-full bg-bone/80 px-3 py-1 text-xs font-semibold text-forest-deep"><Clock size={13} /> {guide.minutes} min read</p>
            <h1 className="mt-5 max-w-3xl font-display text-5xl leading-[0.98] md:text-7xl">{guide.title}</h1>
            <p className="mt-4 max-w-2xl text-base leading-relaxed opacity-85 md:text-lg">{guide.summary}</p>
          </div>
          <span className="hidden opacity-70 md:block"><GuideIcon icon={guide.icon} size={120} /></span>
        </header>

        <div className="mx-auto mt-10 max-w-2xl md:mt-14">
          {guide.youNeed && (
            <aside className="mb-10 rounded-[1.5rem] border border-line bg-white/60 p-6">
              <h2 className="text-sm font-semibold">You&rsquo;ll need</h2>
              <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                {guide.youNeed.map((item) => <li key={item} className="flex gap-2 text-sm text-ink-soft"><Check size={16} className="mt-0.5 shrink-0 text-moss" />{item}</li>)}
              </ul>
            </aside>
          )}
          {guide.sections.map((s, i) => (
            <section key={s.heading} className="mb-10">
              <h2 className="flex items-baseline gap-3 font-display text-3xl md:text-4xl"><span className="text-base font-sans font-semibold text-moss tabular-nums">{String(i + 1).padStart(2, "0")}</span>{s.heading}</h2>
              {s.body.map((p, j) => <p key={j} className="mt-3 text-[1.0625rem] leading-relaxed text-ink-soft">{p}</p>)}
              {s.tips && (
                <ul className="mt-5 space-y-2 rounded-[1.25rem] bg-lime/40 p-5">
                  {s.tips.map((t) => <li key={t} className="flex gap-2.5 text-sm"><Lightbulb size={16} className="mt-0.5 shrink-0 text-forest" />{t}</li>)}
                </ul>
              )}
            </section>
          ))}
          <p className="rounded-[1.5rem] bg-mist p-6 text-sm text-ink-soft">
            Not sure about your plant? Message our plant doctor on WhatsApp ({site.whatsapp}) with a photo. Every plant comes with a {site.guaranteeDays}-day healthy-plant guarantee.
          </p>
        </div>
      </article>

      {plants.length > 0 && (
        <section className="container-page pt-20 md:pt-24">
          <SectionHeading kicker="Ready to plant" title={<>Plants for this <em>guide</em></>} href={shopHref} />
          <ProductRail products={plants} />
        </section>
      )}

      <section className="container-page pt-20 md:pt-24">
        <SectionHeading kicker="Keep reading" title="More guides" href="/guides" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{more.map((g) => <GuideCard key={g.slug} guide={g} />)}</div>
      </section>

      <JsonLd data={{
        "@context": "https://schema.org",
        "@type": "Article",
        headline: guide.title,
        description: guide.summary,
        mainEntityOfPage: absolute(`/guides/${guide.slug}`),
        author: { "@type": "Organization", name: site.name },
        publisher: { "@type": "Organization", name: site.name },
      }} />
      <JsonLd data={breadcrumbs([{ name: "Home", path: "/" }, { name: "Guides", path: "/guides" }, { name: guide.title, path: `/guides/${guide.slug}` }])} />
    </>
  );
}
