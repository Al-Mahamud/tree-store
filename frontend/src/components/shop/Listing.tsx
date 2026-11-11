import Link from "next/link";
import { Leaf } from "lucide-react";
import { api } from "@/lib/api";
import { productQuery, type SearchParams } from "@/lib/listing";
import { ActiveFilters, FilterPanel, MobileFilters, SortSelect } from "./Filters";
import { LoadMoreGrid } from "./LoadMoreGrid";

type Crumb = { name: string; href: string };

/* eslint-disable @next/next/no-img-element */
/** The plant grid page shared by /shop, categories, collections and search. */
export async function Listing({
  scope,
  searchParams,
  kicker,
  title,
  description,
  crumbs = [],
  tabs,
  image,
}: {
  scope: Record<string, string>;
  searchParams: SearchParams;
  kicker?: string;
  title: React.ReactNode;
  description?: string | null;
  crumbs?: Crumb[];
  tabs?: { name: string; href: string; active: boolean }[];
  image?: string | null;
}) {
  const query = productQuery(scope, searchParams);
  const page = await api.products(query);
  return (
    <>
      <section className="px-2 pt-2 md:px-3 md:pt-3">
        <div className={`relative grid overflow-hidden rounded-[2rem] bg-mist md:rounded-[2.5rem] ${image ? "md:grid-cols-[1.1fr_1fr]" : ""}`}>
          <div className="container-page relative z-10 py-10 md:py-14">
            <nav aria-label="Breadcrumb" className="text-xs text-muted">
              <ol className="flex flex-wrap items-center gap-1.5">
                <li><Link href="/" className="hover:text-ink">Home</Link></li>
                {crumbs.map((c) => (
                  <li key={c.href} className="flex items-center gap-1.5"><span aria-hidden>/</span><Link href={c.href} className="hover:text-ink">{c.name}</Link></li>
                ))}
              </ol>
            </nav>
            {kicker && <p className="kicker mt-8 text-moss">{kicker}</p>}
            <h1 className="mt-3 font-display text-5xl leading-[0.95] md:text-7xl">{title}</h1>
            {description && <p className="mt-4 max-w-xl text-sm leading-relaxed text-ink-soft md:text-base">{description}</p>}
            {tabs && tabs.length > 0 && (
              <div className="no-scrollbar -mx-4 mt-8 flex gap-2 overflow-x-auto px-4 md:mx-0 md:flex-wrap md:px-0">
                {tabs.map((t) => (
                  <Link key={t.href} href={t.href} aria-current={t.active ? "page" : undefined} className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold transition ${t.active ? "bg-forest text-bone" : "bg-white/70 hover:bg-white"}`}>
                    {t.name}
                  </Link>
                ))}
              </div>
            )}
          </div>
          {image && (
            <div className="relative hidden min-h-[20rem] md:block">
              <img src={image} alt="" className="absolute inset-0 h-full w-full object-cover" />
            </div>
          )}
        </div>
      </section>

      <div className="container-page">
        <div className="sticky top-16 z-30 -mx-4 flex items-center justify-between gap-3 bg-bone/90 px-4 py-3 backdrop-blur md:top-[4.5rem] md:mx-0 md:px-0">
          <div className="flex items-center gap-3">
            <MobileFilters facets={page.facets} total={page.total} />
            <p className="text-sm text-muted"><span className="font-semibold text-ink">{page.total}</span> {page.total === 1 ? "plant" : "plants"}</p>
          </div>
          <SortSelect />
        </div>

        <div className="grid gap-8 pt-4 lg:grid-cols-[17rem_1fr] xl:gap-12">
          <aside className="hidden lg:block" aria-label="Filters">
            <div className="no-scrollbar sticky top-36 max-h-[calc(100dvh-10rem)] overflow-y-auto pb-6">
              <FilterPanel facets={page.facets} />
            </div>
          </aside>
          <div>
            <ActiveFilters facets={page.facets} />
            {page.items.length === 0 ? (
              <div className="grid place-items-center rounded-[2rem] border border-dashed border-line px-6 py-24 text-center">
                <Leaf size={28} strokeWidth={1.5} className="text-moss" />
                <p className="mt-4 font-display text-3xl">No plants match all of that</p>
                <p className="mt-2 text-sm text-muted">Try removing a filter — or ask our plant doctor on WhatsApp for a suggestion.</p>
              </div>
            ) : (
              <LoadMoreGrid key={query.toString()} initial={page} query={query.toString()} />
            )}
          </div>
        </div>
      </div>
    </>
  );
}
