"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Plus, Search, Star } from "lucide-react";
import { request } from "@/lib/client";
import type { AdminProductPage, CategoryAdmin } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { Empty, FilterTabs, Loading, PageHeader, Pager, btn, input } from "@/components/admin/ui";

const tabs = [
  { value: "", label: "All" },
  { value: "ACTIVE", label: "On sale", count: "ACTIVE" },
  { value: "DRAFT", label: "Drafts", count: "DRAFT" },
  { value: "ARCHIVED", label: "Archived", count: "ARCHIVED" },
];
const statusTone = { ACTIVE: "bg-ok/15 text-ok", DRAFT: "bg-lime/50 text-forest-deep", ARCHIVED: "bg-mist text-muted" } as const;
const statusWord = { ACTIVE: "On sale", DRAFT: "Draft", ARCHIVED: "Archived" } as const;

/* eslint-disable @next/next/no-img-element */
function Plants() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const status = params.get("status") ?? "";
  const category = params.get("category") ?? "";
  const low = params.get("low") === "1";
  const q = params.get("q") ?? "";
  const page = Number(params.get("page") ?? "0");
  const [data, setData] = useState<AdminProductPage | null>(null);
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [search, setSearch] = useState(q);

  useEffect(() => {
    request<CategoryAdmin[]>("GET", "/admin/categories").then(setCategories).catch(() => undefined);
  }, []);
  useEffect(() => {
    const query = new URLSearchParams({ page: String(page) });
    if (status) query.set("status", status);
    if (category) query.set("category", category);
    if (low) query.set("low", "true");
    if (q) query.set("q", q);
    request<AdminProductPage>("GET", `/admin/products?${query}`).then(setData).catch(() => setData(null));
  }, [status, category, low, q, page]);

  const go = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    if (!("page" in changes)) next.delete("page");
    router.push(`${pathname}?${next}`);
  };

  return (
    <>
      <PageHeader title="Plants" note="Everything in the nursery's catalogue.">
        <Link href="/admin/products/new" className={btn.primary}><Plus size={16} /> New plant</Link>
      </PageHeader>
      <FilterTabs tabs={tabs} value={status} counts={data?.counts} onChange={(v) => go({ status: v })} />
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); go({ q: search.trim() }); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Search plants</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Name, Bangla or botanical name, SKU" className={`${input} pl-9`} /></label>
        <select aria-label="Category" value={category} onChange={(e) => go({ category: e.target.value })} className={`${input} w-auto!`}>
          <option value="">Every category</option>
          {categories.map((c) => [<option key={c.id} value={c.id}>{c.name}</option>, ...c.children.map((k) => <option key={k.id} value={k.id}>— {k.name}</option>)])}
        </select>
        <label className="flex items-center gap-2 rounded-full bg-white px-3.5 text-sm font-semibold ring-1 ring-line"><input type="checkbox" checked={low} onChange={(e) => go({ low: e.target.checked ? "1" : "" })} className="h-4 w-4 accent-forest" /> Running low</label>
        <button type="submit" className={btn.primary}>Search</button>
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No plants match.</Empty> : (
        <ul className="divide-y divide-line overflow-hidden rounded-3xl border border-line bg-white">
          {data.items.map((p) => (
            <li key={p.id}>
              <Link href={`/admin/products/${p.id}`} className={`grid grid-cols-[3.5rem_1fr_auto] items-center gap-3 px-3 py-2.5 transition hover:bg-bone/70 md:grid-cols-[3.5rem_1fr_9rem_7rem_7rem_6rem] md:px-4 ${p.status === "ARCHIVED" ? "opacity-60" : ""}`}>
                <span className="h-14 w-14 overflow-hidden rounded-2xl bg-mist">{p.image && <img src={p.image} alt="" className="h-full w-full object-cover" />}</span>
                <span className="min-w-0">
                  <span className="flex items-center gap-1.5 font-semibold">{p.name}{p.featured && <Star size={13} className="fill-lime text-forest" aria-label="Featured" />}</span>
                  <span className="block truncate text-xs text-muted">{[p.localName, p.botanicalName].filter(Boolean).join(" · ")}</span>
                  <span className="text-xs text-muted md:hidden">{p.category}</span>
                </span>
                <span className="hidden text-sm text-ink-soft md:block">{p.category}</span>
                <span className="hidden text-sm md:block"><span className="font-semibold tabular-nums">from {taka(p.fromPrice)}</span><span className="block text-xs text-muted">{p.optionCount} option{p.optionCount === 1 ? "" : "s"}</span></span>
                <span className="hidden text-sm md:block"><span className={`font-semibold tabular-nums ${p.low ? "text-clay" : ""}`}>{p.stock} in stock</span><span className="block text-xs text-muted">{p.soldCount} sold</span></span>
                <span className={`justify-self-end rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusTone[p.status]}`}>{statusWord[p.status]}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {data && <Pager page={page} pageSize={data.pageSize} total={data.total} onPage={(p) => go({ page: String(p) })} />}
    </>
  );
}

export default function PlantsPage() {
  return <Suspense fallback={<Loading />}><Plants /></Suspense>;
}
