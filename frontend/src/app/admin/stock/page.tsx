"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Search, Truck } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Size, StockRow } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { Empty, Loading, PageHeader, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const LOW = 3;
const sizes: { value: Size; label: string }[] = [{ value: "SMALL", label: "Small" }, { value: "MEDIUM", label: "Medium" }, { value: "LARGE", label: "Large" }];

/** One number box; saves when it loses focus or Enter is pressed. */
function StockBox({ row, onSaved }: { row: StockRow; onSaved: (row: StockRow) => void }) {
  const { notify } = useAdmin();
  const [value, setValue] = useState(String(row.stock));
  const [state, setState] = useState<"idle" | "saving" | "error">("idle");
  const save = async () => {
    if (value.trim() === String(row.stock)) return;
    setState("saving");
    try {
      const saved = await request<StockRow>("PATCH", `/admin/variants/${row.variantId}/stock`, { stock: Number(value) });
      onSaved(saved);
      setValue(String(saved.stock));
      setState("idle");
      notify(`${row.product}, ${row.size.toLowerCase()} in ${row.planter}: ${saved.stock} in stock`);
    } catch (e) {
      setState("error");
      notify(e instanceof ApiError ? (Object.values(e.fields)[0] ?? e.message) : "Couldn't save.");
    }
  };
  const tone = state === "error" ? "border-sale" : row.stock === 0 ? "border-sale/40 bg-sale/5" : row.stock <= LOW ? "border-clay/50 bg-clay/10" : "";
  return (
    <span className="block">
      <input
        aria-label={`Stock of ${row.product}, ${row.size.toLowerCase()}, ${row.planter}`}
        inputMode="numeric"
        value={value}
        onChange={(e) => { setValue(e.target.value.replace(/[^0-9]/g, "")); setState("idle"); }}
        onBlur={save}
        onKeyDown={(e) => e.key === "Enter" && (e.target as HTMLInputElement).blur()}
        className={`${input} mx-auto block max-w-20 px-1 text-center tabular-nums ${tone} ${state === "saving" ? "opacity-60" : ""}`}
      />
      <span className="mt-0.5 flex items-center justify-center gap-1 text-[0.7rem] text-muted">{taka(row.price)}{row.dhakaOnly && <Truck size={11} className="text-clay" aria-label="Dhaka only" />}</span>
    </span>
  );
}

/* eslint-disable @next/next/no-img-element */
function Stock() {
  const params = useSearchParams();
  const [low, setLow] = useState(params.get("low") === "1");
  const [q, setQ] = useState("");
  const [search, setSearch] = useState("");
  const [rows, setRows] = useState<StockRow[] | null>(null);

  useEffect(() => {
    const query = new URLSearchParams();
    if (low) query.set("low", "true");
    if (q) query.set("q", q);
    request<StockRow[]>("GET", `/admin/stock?${query}`).then(setRows).catch(() => setRows([]));
  }, [low, q]);

  // Plant → planter → size, so each plant reads as a small grid.
  const plants = useMemo(() => {
    const byPlant = new Map<number, { id: number; name: string; image: string | null; status: string; planters: Map<string, Partial<Record<Size, StockRow>>> }>();
    for (const r of rows ?? []) {
      const plant = byPlant.get(r.productId) ?? { id: r.productId, name: r.product, image: r.image, status: r.status, planters: new Map() };
      const planter = plant.planters.get(r.planter) ?? {};
      planter[r.size] = r;
      plant.planters.set(r.planter, planter);
      byPlant.set(r.productId, plant);
    }
    return [...byPlant.values()];
  }, [rows]);

  const replace = (saved: StockRow) => setRows((list) => list?.map((r) => (r.variantId === saved.variantId ? saved : r)) ?? null);

  return (
    <>
      <PageHeader title="Stock" note="Plants ready to send, per size and planter. Type a number and press Enter." />
      <div className="mb-4 flex flex-wrap gap-2">
        <form className="relative min-w-60 flex-1" onSubmit={(e) => { e.preventDefault(); setQ(search.trim()); }}>
          <label><span className="sr-only">Search</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Plant name or SKU" className={`${input} pl-9`} /></label>
        </form>
        <label className="flex items-center gap-2 rounded-full bg-white px-3.5 text-sm font-semibold ring-1 ring-line"><input type="checkbox" checked={low} onChange={(e) => setLow(e.target.checked)} className="h-4 w-4 accent-forest" /> Only running low ({LOW} or fewer)</label>
      </div>
      {!rows ? <Loading /> : plants.length === 0 ? <Empty>{low ? "Nothing is running low." : "No plants match."}</Empty> : (
        <ul className="grid gap-3 lg:grid-cols-2">
          {plants.map((p) => (
            <li key={p.id} className="rounded-3xl border border-line bg-white p-4">
              <div className="mb-3 flex items-center gap-3">
                <span className="h-11 w-11 shrink-0 overflow-hidden rounded-2xl bg-mist">{p.image && <img src={p.image} alt="" className="h-full w-full object-cover" />}</span>
                <Link href={`/admin/products/${p.id}`} className="min-w-0 flex-1 truncate font-semibold hover:text-moss">{p.name}</Link>
                {p.status === "DRAFT" && <span className="rounded-full bg-lime/50 px-2.5 py-0.5 text-xs font-semibold">Draft</span>}
              </div>
              <div className="overflow-x-auto">
                <table className="w-full table-fixed text-sm">
                  <thead><tr className="text-xs text-muted"><th className="w-[30%] pb-1.5 sm:w-[34%] text-left font-semibold">Planter</th>{sizes.map((s) => <th key={s.value} className="pb-1.5 font-semibold">{s.label}</th>)}</tr></thead>
                  <tbody>
                    {[...p.planters.entries()].map(([planter, cells]) => (
                      <tr key={planter} className="border-t border-line">
                        <td className="py-2 pr-2 font-semibold text-ink-soft">{planter}</td>
                        {sizes.map((s) => (
                          <td key={s.value} className="py-2 text-center align-top">
                            {cells[s.value] ? <StockBox key={cells[s.value]!.variantId} row={cells[s.value]!} onSaved={replace} /> : <span className="text-xs text-muted">—</span>}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export default function StockPage() {
  return <Suspense fallback={<Loading />}><Stock /></Suspense>;
}
