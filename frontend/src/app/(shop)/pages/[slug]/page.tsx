import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Truck } from "lucide-react";
import { pages } from "@/config/pages";
import { api } from "@/lib/api";
import { taka } from "@/lib/money";

type Props = { params: Promise<{ slug: string }> };

export function generateStaticParams() {
  return pages.map((p) => ({ slug: p.slug }));
}

export const dynamicParams = false;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const page = pages.find((p) => p.slug === slug);
  if (!page) return {};
  return { title: page.title, description: page.description, alternates: { canonical: `/pages/${page.slug}` } };
}

export default async function InfoPage({ params }: Props) {
  const { slug } = await params;
  const page = pages.find((p) => p.slug === slug);
  if (!page) notFound();
  const fees = slug === "delivery" ? await api.delivery().catch(() => null) : null;
  return (
    <div className="container-page grid gap-10 pt-10 md:grid-cols-[15rem_minmax(0,1fr)] md:pt-16">
      <nav aria-label="Help and information" className="order-2 md:order-1">
        <ul className="flex flex-wrap gap-1.5 md:sticky md:top-24 md:flex-col">
          {pages.map((p) => (
            <li key={p.slug}>
              <Link
                href={`/pages/${p.slug}`}
                aria-current={p.slug === slug ? "page" : undefined}
                className={`block rounded-full px-4 py-2 text-sm transition ${p.slug === slug ? "bg-forest font-semibold text-bone" : "bg-mist text-ink-soft hover:text-ink"}`}
              >
                {p.title}
              </Link>
            </li>
          ))}
          <li><Link href="/guides" className="block rounded-full bg-mist px-4 py-2 text-sm text-ink-soft hover:text-ink">Planting guides</Link></li>
        </ul>
      </nav>
      <article className="order-1 max-w-2xl md:order-2">
        <h1 className="font-display text-5xl leading-[0.98] md:text-6xl">{page.title}</h1>
        <p className="mt-5 text-lg leading-relaxed text-ink-soft">{page.intro}</p>
        {fees && (
          <section className="mt-8 grid gap-3 sm:grid-cols-3" aria-label="Delivery fees">
            {[
              ["Inside Dhaka", taka(fees.insideDhaka), "1–2 days"],
              ["Outside Dhaka", taka(fees.outsideDhaka), "2–4 days"],
              ["Free delivery", `over ${taka(fees.freeOver)}`, "after discounts"],
            ].map(([label, value, note]) => (
              <div key={label} className="rounded-[1.25rem] bg-mist p-4">
                <Truck size={18} className="text-moss" strokeWidth={1.7} />
                <p className="mt-2 text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
                <p className="mt-0.5 text-lg font-semibold">{value}</p>
                <p className="text-xs text-muted">{note}</p>
              </div>
            ))}
          </section>
        )}
        {page.sections.map((s) => (
          <section key={s.heading} className="mt-10">
            <h2 className="font-display text-3xl">{s.heading}</h2>
            {s.body.map((para, i) => <p key={i} className="mt-3 leading-relaxed text-ink-soft">{para}</p>)}
          </section>
        ))}
      </article>
    </div>
  );
}
