"use client";

import { useState, useTransition } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Check, ChevronDown, PawPrint, SlidersHorizontal, X } from "lucide-react";
import type { ProductPage } from "@/lib/types";
import type { Light } from "@/lib/care";
import { PRICE_BANDS, SORTS } from "@/lib/listing";
import { Drawer } from "@/components/ui/Drawer";
import { LightIcon } from "@/components/ui/CareIcon";

type Facets = ProductPage["facets"];

const sizeHints: Record<string, string> = { SMALL: "Starter", MEDIUM: "Established", LARGE: "Instant impact" };

/** Reads and changes filters in the address bar, so filtered pages can be shared. */
function useFilters() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [pending, startTransition] = useTransition();
  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    const search = next.toString();
    startTransition(() => router.replace(search ? `${pathname}?${search}` : pathname, { scroll: false }));
  };
  const list = (key: string) => (params.get(key) ?? "").split(",").filter(Boolean).map((v) => v.toUpperCase());
  const toggle = (key: string, value: string) => {
    const current = list(key);
    const next = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
    update({ [key]: next.map((v) => v.toLowerCase()).join(",") || null });
  };
  return { params, update, list, toggle, pending };
}

export function FilterPanel({ facets }: { facets: Facets }) {
  const { params, update, toggle, pending } = useFilters();
  const placement = params.get("placement")?.toUpperCase() ?? null;
  const pets = params.get("petFriendly") === "true";
  const sale = params.get("sale") === "true";
  const min = params.get("minPrice");
  const max = params.get("maxPrice");
  const any = ["light", "care", "placement", "petFriendly", "size", "minPrice", "maxPrice", "sale"].some((k) => params.get(k));

  return (
    <div className={`space-y-2 transition-opacity ${pending ? "opacity-60" : ""}`}>
      <Section title="Where will it live?">
        <div className="grid grid-cols-2 gap-2">
          {facets.placement.map((p) => (
            <Chip key={p.value} active={placement === p.value} onClick={() => update({ placement: placement === p.value ? null : p.value.toLowerCase() })}>
              {p.label} <span className="opacity-60">{p.count}</span>
            </Chip>
          ))}
        </div>
      </Section>

      <Section title="Light">
        <ul className="space-y-1">
          {facets.light.map((l) => (
            <li key={l.value}>
              <Row active={l.selected} onClick={() => toggle("light", l.value)} count={l.count}>
                <LightIcon light={l.value as Light} /> {l.label}
              </Row>
            </li>
          ))}
        </ul>
      </Section>

      <Section title="Care level">
        <ul className="space-y-1">
          {facets.care.map((c) => (
            <li key={c.value}>
              <Row active={c.selected} onClick={() => toggle("care", c.value)} count={c.count}>{c.label}</Row>
            </li>
          ))}
        </ul>
      </Section>

      <Section title="Size">
        <div className="grid grid-cols-3 gap-2">
          {facets.sizes.map((s) => (
            <button
              key={s.value}
              type="button"
              aria-pressed={s.selected}
              onClick={() => toggle("size", s.value)}
              className={`rounded-2xl border px-2 py-2.5 text-center transition ${s.selected ? "border-forest bg-forest text-bone" : "border-line bg-white/60 hover:border-forest"}`}
            >
              <span className="block text-sm font-semibold">{s.label}</span>
              <span className={`block text-[0.625rem] ${s.selected ? "text-bone/70" : "text-muted"}`}>{sizeHints[s.value]}</span>
            </button>
          ))}
        </div>
      </Section>

      <Section title="Budget">
        <div className="flex flex-wrap gap-2">
          {PRICE_BANDS.map((band) => {
            const active = (min ?? "") === String(band.min ?? "") && (max ?? "") === String(band.max ?? "");
            return (
              <Chip key={band.label} active={active} onClick={() => update(active ? { minPrice: null, maxPrice: null } : { minPrice: band.min?.toString() ?? null, maxPrice: band.max?.toString() ?? null })}>
                {band.label}
              </Chip>
            );
          })}
        </div>
      </Section>

      <Section title="More">
        <div className="space-y-2">
          <Toggle checked={pets} onChange={() => update({ petFriendly: pets ? null : "true" })}>
            <PawPrint size={15} /> Safe for pets <span className="ml-auto text-xs text-muted">{facets.petFriendly}</span>
          </Toggle>
          <Toggle checked={sale} onChange={() => update({ sale: sale ? null : "true" })}>On offer</Toggle>
        </div>
      </Section>

      {any && (
        <button type="button" onClick={() => update({ light: null, care: null, placement: null, petFriendly: null, size: null, minPrice: null, maxPrice: null, sale: null })} className="mt-3 text-sm font-semibold text-moss underline underline-offset-4">
          Clear all filters
        </button>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  const [open, setOpen] = useState(true);
  return (
    <div className="rounded-[1.5rem] bg-white/50 p-4">
      <button type="button" aria-expanded={open} onClick={() => setOpen(!open)} className="flex w-full items-center justify-between text-left">
        <span className="text-sm font-semibold">{title}</span>
        <ChevronDown size={16} className={`transition-transform ${open ? "rotate-180" : ""}`} />
      </button>
      {open && <div className="pt-3">{children}</div>}
    </div>
  );
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" aria-pressed={active} onClick={onClick} className={`inline-flex items-center justify-center gap-1.5 rounded-full border px-3.5 py-2 text-sm transition ${active ? "border-forest bg-forest text-bone" : "border-line bg-white/60 hover:border-forest"}`}>
      {children}
    </button>
  );
}

function Row({ active, onClick, count, children }: { active: boolean; onClick: () => void; count: number; children: React.ReactNode }) {
  return (
    <button type="button" aria-pressed={active} onClick={onClick} className={`flex w-full items-center gap-2.5 rounded-xl px-2 py-2 text-left text-sm transition ${active ? "bg-lime/50 font-semibold" : "hover:bg-mist"}`}>
      <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-md border ${active ? "border-forest bg-forest text-bone" : "border-muted/40 bg-white"}`}>
        {active && <Check size={12} strokeWidth={3} />}
      </span>
      <span className="flex items-center gap-1.5">{children}</span>
      <span className="ml-auto text-xs text-muted">{count}</span>
    </button>
  );
}

function Toggle({ checked, onChange, children }: { checked: boolean; onChange: () => void; children: React.ReactNode }) {
  return (
    <label className="flex cursor-pointer items-center gap-3 rounded-xl px-2 py-2 text-sm hover:bg-mist">
      <input type="checkbox" checked={checked} onChange={onChange} className="peer sr-only" />
      <span className={`relative h-5 w-9 shrink-0 rounded-full transition ${checked ? "bg-forest" : "bg-sage"}`}>
        <span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-all ${checked ? "left-[1.125rem]" : "left-0.5"}`} />
      </span>
      <span className="flex flex-1 items-center gap-1.5">{children}</span>
    </label>
  );
}

export function MobileFilters({ facets, total }: { facets: Facets; total: number }) {
  const [open, setOpen] = useState(false);
  const { params } = useFilters();
  const count = ["light", "care", "size"].reduce((n, k) => n + (params.get(k)?.split(",").filter(Boolean).length ?? 0), 0)
    + ["placement", "petFriendly", "sale"].filter((k) => params.get(k)).length + (params.get("minPrice") || params.get("maxPrice") ? 1 : 0);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)} className="inline-flex items-center gap-2 rounded-full bg-forest px-4 py-2 text-sm font-semibold text-bone lg:hidden">
        <SlidersHorizontal size={15} /> Filters {count > 0 && <span className="grid h-5 min-w-5 place-items-center rounded-full bg-lime px-1 text-[0.625rem] text-forest-deep">{count}</span>}
      </button>
      <Drawer open={open} onClose={() => setOpen(false)} side="left" label="Filters">
        <div className="flex h-16 items-center justify-between px-5">
          <span className="font-display text-2xl">Filters</span>
          <button type="button" aria-label="Close filters" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-full bg-mist"><X size={18} /></button>
        </div>
        <div className="flex-1 overflow-y-auto px-4"><FilterPanel facets={facets} /></div>
        <div className="p-4">
          <button type="button" onClick={() => setOpen(false)} className="w-full rounded-full bg-forest py-4 text-sm font-semibold text-bone">Show {total} {total === 1 ? "plant" : "plants"}</button>
        </div>
      </Drawer>
    </>
  );
}

export function SortSelect() {
  const { params, update } = useFilters();
  return (
    <label className="relative inline-flex items-center rounded-full border border-line bg-white/60 text-sm">
      <span className="sr-only">Sort by</span>
      <select value={params.get("sort") ?? "recommended"} onChange={(e) => update({ sort: e.target.value === "recommended" ? null : e.target.value })} className="appearance-none bg-transparent py-2 pl-4 pr-9 font-semibold outline-none">
        {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
      </select>
      <ChevronDown size={15} className="pointer-events-none absolute right-3" />
    </label>
  );
}

export function ActiveFilters({ facets }: { facets: Facets }) {
  const { params, update, toggle, list } = useFilters();
  const chips: { label: string; remove: () => void }[] = [];
  for (const v of list("light")) chips.push({ label: facets.light.find((f) => f.value === v)?.label ?? v, remove: () => toggle("light", v) });
  for (const v of list("care")) chips.push({ label: facets.care.find((f) => f.value === v)?.label ?? v, remove: () => toggle("care", v) });
  for (const v of list("size")) chips.push({ label: facets.sizes.find((f) => f.value === v)?.label ?? v, remove: () => toggle("size", v) });
  const placement = params.get("placement");
  if (placement) chips.push({ label: placement === "indoor" ? "Indoor" : "Outdoor", remove: () => update({ placement: null }) });
  if (params.get("petFriendly")) chips.push({ label: "Pet-safe", remove: () => update({ petFriendly: null }) });
  const min = params.get("minPrice");
  const max = params.get("maxPrice");
  if (min || max) chips.push({ label: min && max ? `৳${min}–৳${max}` : min ? `Over ৳${min}` : `Under ৳${Number(max) + 1}`, remove: () => update({ minPrice: null, maxPrice: null }) });
  if (params.get("sale")) chips.push({ label: "On offer", remove: () => update({ sale: null }) });
  if (chips.length === 0) return null;
  return (
    <div className="mb-6 flex flex-wrap gap-2">
      {chips.map((chip) => (
        <button key={chip.label} type="button" onClick={chip.remove} aria-label={`Remove ${chip.label}`} className="inline-flex items-center gap-1.5 rounded-full bg-lime/60 px-3 py-1.5 text-xs font-semibold hover:bg-lime">
          {chip.label} <X size={12} />
        </button>
      ))}
    </div>
  );
}
