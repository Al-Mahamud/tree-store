"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowDown, ArrowLeft, ArrowUp, ExternalLink, ImagePlus, Plus, Trash2, Truck, X } from "lucide-react";
import { ApiError, request, upload as send } from "@/lib/client";
import type { AdminImage, AdminProduct, CategoryAdmin, CollectionAdmin, Growth, PlanterKind, ProductStatus, Size } from "@/lib/admin-types";
import { careLabels, growthLabels, lightLabels, placementLabels, waterLabels, type CareLevel, type Light, type Placement, type Water } from "@/lib/care";
import { taka } from "@/lib/money";
import { Card, Loading, btn, input } from "./ui";
import { useAdmin } from "./AdminShell";

const SIZES: { value: Size; label: string; height: string }[] = [
  { value: "SMALL", label: "Small", height: "1–2 ft" },
  { value: "MEDIUM", label: "Medium", height: "3–4 ft" },
  { value: "LARGE", label: "Large", height: "5–6 ft" },
];

const KINDS: { value: PlanterKind; label: string; hex: string; code: string }[] = [
  { value: "GROW_BAG", label: "Grow bag", hex: "#262626", code: "GB" },
  { value: "CLAY", label: "Clay pot", hex: "#B8643C", code: "CL" },
  { value: "CERAMIC", label: "Ceramic pot", hex: "#EDEAE2", code: "CE" },
];

const STATUSES: { value: ProductStatus; label: string; note: string }[] = [
  { value: "DRAFT", label: "Draft", note: "Hidden from the shop" },
  { value: "ACTIVE", label: "On sale", note: "In the shop now" },
  { value: "ARCHIVED", label: "Archived", note: "Kept for old orders" },
];

type PlanterForm = { key: string; id: number | null; name: string; kind: PlanterKind; hex: string };
type Cell = { id: number | null; on: boolean; height: string; sku: string; price: string; compareAtPrice: string; stock: string; dhakaOnly: boolean };
type Form = {
  name: string; localName: string; botanicalName: string; slug: string; categoryId: string;
  description: string; details: string; light: Light; water: Water; careLevel: CareLevel; placement: Placement;
  petFriendly: boolean; matureHeight: string; growthRate: Growth | ""; fruiting: string; featured: boolean; status: ProductStatus;
  collectionIds: number[]; planters: PlanterForm[]; cells: Record<string, Cell>;
};

let counter = 0;
const newKey = () => `new-${++counter}`;
const cellKey = (planter: string, size: Size) => `${planter}:${size}`;
const blankCell = (size: Size): Cell => ({ id: null, on: false, height: SIZES.find((s) => s.value === size)!.height, sku: "", price: "", compareAtPrice: "", stock: "0", dhakaOnly: size === "LARGE" });

function toForm(p: AdminProduct | null): Form {
  const planters: PlanterForm[] = p ? p.planters.map((pl) => ({ key: `id-${pl.id}`, id: pl.id, name: pl.name, kind: pl.kind, hex: pl.hex })) : [{ key: newKey(), id: null, name: "Grow bag", kind: "GROW_BAG", hex: "#262626" }];
  const cells: Record<string, Cell> = {};
  for (const pl of planters) for (const s of SIZES) cells[cellKey(pl.key, s.value)] = blankCell(s.value);
  for (const v of p?.variants ?? []) {
    cells[cellKey(`id-${v.planterId}`, v.size)] = { id: v.id, on: true, height: v.height, sku: v.sku, price: String(v.price), compareAtPrice: v.compareAtPrice ? String(v.compareAtPrice) : "", stock: String(v.stock), dhakaOnly: v.dhakaOnly };
  }
  return {
    name: p?.name ?? "", localName: p?.localName ?? "", botanicalName: p?.botanicalName ?? "", slug: p?.slug ?? "", categoryId: p ? String(p.categoryId) : "",
    description: p?.description ?? "", details: p?.details ?? "", light: p?.light ?? "BRIGHT", water: p?.water ?? "MEDIUM", careLevel: p?.careLevel ?? "EASY",
    placement: p?.placement ?? "OUTDOOR", petFriendly: p?.petFriendly ?? false, matureHeight: p?.matureHeight ?? "", growthRate: p?.growthRate ?? "",
    fruiting: p?.fruiting ?? "", featured: p?.featured ?? false, status: p?.status ?? "DRAFT", collectionIds: p?.collectionIds ?? [], planters, cells,
  };
}

/** Builds the request and remembers which cell each option came from, so errors land on the right field. */
function toBody(form: Form) {
  const order: string[] = [];
  const variants = form.planters.flatMap((pl, index) =>
    SIZES.flatMap(({ value: size }) => {
      const c = form.cells[cellKey(pl.key, size)];
      if (!c?.on) return [];
      order.push(cellKey(pl.key, size));
      const n = (v: string) => (v.trim() === "" ? null : Number(v));
      return [{ id: c.id, planter: index, size, height: c.height, sku: c.sku, price: n(c.price), compareAtPrice: n(c.compareAtPrice), stock: n(c.stock), dhakaOnly: c.dhakaOnly }];
    }),
  );
  const body = {
    name: form.name, localName: form.localName, botanicalName: form.botanicalName, slug: form.slug, categoryId: form.categoryId ? Number(form.categoryId) : null,
    description: form.description, details: form.details, light: form.light, water: form.water, careLevel: form.careLevel, placement: form.placement,
    petFriendly: form.petFriendly, matureHeight: form.matureHeight, growthRate: form.growthRate || null, fruiting: form.fruiting, featured: form.featured,
    status: form.status, collectionIds: form.collectionIds, planters: form.planters.map((p) => ({ id: p.id, name: p.name, kind: p.kind, hex: p.hex })), variants,
  };
  return { body, order };
}

function slugify(text: string) {
  return text.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

/** A short SKU from the plant's name, the size and the planter, e.g. AMRAPALI-M-CL. */
function suggestSku(name: string, size: Size, planter: PlanterForm, planters: PlanterForm[]) {
  const base = slugify(name).split("-").filter(Boolean).slice(0, 2).join("-").toUpperCase() || "PLANT";
  const code = KINDS.find((k) => k.value === planter.kind)!.code;
  const sameKind = planters.filter((p) => p.kind === planter.kind);
  const suffix = sameKind.length > 1 ? String(sameKind.indexOf(planter) + 1) : "";
  return `${base}-${size.charAt(0)}-${code}${suffix}`;
}

/* eslint-disable @next/next/no-img-element */
export function ProductEditor({ id }: { id: number | null }) {
  const router = useRouter();
  const { notify } = useAdmin();
  const [product, setProduct] = useState<AdminProduct | null>(null);
  const [form, setForm] = useState<Form | null>(id === null ? toForm(null) : null);
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [collections, setCollections] = useState<CollectionAdmin[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    request<CategoryAdmin[]>("GET", "/admin/categories").then(setCategories).catch(() => undefined);
    request<CollectionAdmin[]>("GET", "/admin/collections").then(setCollections).catch(() => undefined);
    if (id !== null) {
      request<AdminProduct>("GET", `/admin/products/${id}`).then((p) => { setProduct(p); setForm(toForm(p)); }).catch(() => notify("Couldn't load the plant."));
    }
  }, [id, notify]);

  useEffect(() => {
    if (!dirty) return;
    const warn = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  if (!form) return <Loading />;

  const set = (changes: Partial<Form>) => {
    setForm({ ...form, ...changes });
    setDirty(true);
  };
  const clear = (...keys: string[]) => setErrors((e) => { const n = { ...e }; keys.forEach((k) => delete n[k]); return n; });
  const setCell = (key: string, changes: Partial<Cell>) => {
    const size = key.slice(key.lastIndexOf(":") + 1) as Size;
    set({ cells: { ...form.cells, [key]: { ...(form.cells[key] ?? blankCell(size)), ...changes } } });
    clear(...Object.keys(changes).map((k) => `${key}:${k}`), "variants");
  };
  const setPlanter = (index: number, changes: Partial<PlanterForm>) => {
    set({ planters: form.planters.map((p, i) => (i === index ? { ...p, ...changes } : p)) });
    clear(...Object.keys(changes).map((k) => `planters.${index}.${k}`));
  };
  const addPlanter = (kind: PlanterKind) => {
    const k = KINDS.find((x) => x.value === kind)!;
    const taken = new Set(form.planters.map((p) => p.name.toLowerCase()));
    let name = k.label;
    for (let n = 2; taken.has(name.toLowerCase()); n++) name = `${k.label} ${n}`;
    const planter = { key: newKey(), id: null, name, kind, hex: k.hex };
    const cells = { ...form.cells };
    for (const s of SIZES) cells[cellKey(planter.key, s.value)] = blankCell(s.value);
    set({ planters: [...form.planters, planter], cells });
    clear("planters");
  };
  const removePlanter = (index: number) => {
    const planter = form.planters[index];
    if (!planter) return;
    const used = SIZES.some((s) => form.cells[cellKey(planter.key, s.value)]?.on);
    if (used && !window.confirm(`Remove ${planter.name} and its sizes? Its pictures stay with the plant.`)) return;
    set({ planters: form.planters.filter((_, i) => i !== index) });
    setErrors({});
  };
  const movePlanter = (index: number, direction: number) => {
    const to = index + direction;
    if (to < 0 || to >= form.planters.length) return;
    const planters = [...form.planters];
    const [moved] = planters.splice(index, 1);
    if (moved) planters.splice(to, 0, moved);
    set({ planters });
    setErrors({});
  };

  const save = async () => {
    const { body, order } = toBody(form);
    setBusy(true);
    try {
      const saved = id === null ? await request<AdminProduct>("POST", "/admin/products", body) : await request<AdminProduct>("PUT", `/admin/products/${id}`, body);
      setDirty(false);
      setErrors({});
      notify(`${saved.name} saved`);
      if (id === null) router.replace(`/admin/products/${saved.id}`);
      else { setProduct(saved); setForm(toForm(saved)); }
    } catch (e) {
      if (e instanceof ApiError) {
        // "variants.3.price" → "<planter>:<size>:price"
        const mapped: Record<string, string> = {};
        for (const [k, v] of Object.entries(e.fields)) {
          const m = k.match(/^variants\.(\d+)\.(\w+)$/);
          mapped[m ? `${order[Number(m[1])]}:${m[2]}` : k] = v;
        }
        setErrors(mapped);
        notify(Object.keys(mapped).length ? "Please check the highlighted fields." : e.message);
      } else notify("Something went wrong.");
    } finally {
      setBusy(false);
    }
  };

  const err = (key: string) => errors[key] && <span className="mt-1 block text-xs font-semibold text-sale">{errors[key]}</span>;
  const text = (label: string, key: keyof Form, props: React.InputHTMLAttributes<HTMLInputElement> & { hint?: string } = {}) => {
    const { hint, ...rest } = props;
    return (
      <label className="block">
        <span className="mb-1 block text-xs font-semibold text-ink-soft">{label}</span>
        <input value={form[key] as string} onChange={(e) => { set({ [key]: e.target.value } as Partial<Form>); clear(key); }} className={`${input} ${errors[key] ? "border-sale" : ""}`} {...rest} />
        {errors[key] ? err(key) : hint && <span className="mt-1 block text-xs text-muted">{hint}</span>}
      </label>
    );
  };
  function choice<T extends string>(label: string, key: keyof Form, options: Record<T, string>) {
    return (
      <fieldset>
        <legend className="mb-1.5 text-xs font-semibold text-ink-soft">{label}</legend>
        <div className="flex flex-wrap gap-1.5">
          {(Object.entries(options) as [T, string][]).map(([value, words]) => (
            <button key={value} type="button" aria-pressed={form![key] === value} onClick={() => { set({ [key]: value } as Partial<Form>); clear(key); }} className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${form![key] === value ? "bg-forest text-bone" : "bg-white text-ink-soft ring-1 ring-line hover:ring-forest/50"}`}>{words}</button>
          ))}
        </div>
        {err(key)}
      </fieldset>
    );
  }

  const flat = categories.flatMap((c) => [{ id: c.id, label: c.name }, ...c.children.map((k) => ({ id: k.id, label: `${c.name} › ${k.name}` }))]);
  const offered = form.planters.reduce((n, p) => n + SIZES.filter((s) => form.cells[cellKey(p.key, s.value)]?.on).length, 0);

  return (
    <div className="pb-24">
      <Link href="/admin/products" className="mb-3 inline-flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink"><ArrowLeft size={15} /> Plants</Link>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-4xl leading-none">{id === null ? "New plant" : product?.name}</h1>
          {product && <p className="mt-1.5 text-sm text-muted">{product.soldCount} sold · {product.category}</p>}
        </div>
        {product?.status === "ACTIVE" && <a href={`/p/${product.slug}`} target="_blank" rel="noreferrer" className={btn.ghost}><ExternalLink size={15} /> View in shop</a>}
      </div>

      <div className="grid gap-4 xl:grid-cols-[1fr_22rem]">
        <div className="space-y-4">
          <Card title="The plant">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2">{text("Name", "name", { placeholder: "e.g. Amrapali Mango" })}</div>
              {text("Bangla name", "localName", { placeholder: "আম্রপালি আম", lang: "bn" })}
              {text("Botanical name", "botanicalName", { placeholder: "Mangifera indica", className: `${input} italic` })}
              <label className="block">
                <span className="mb-1 block text-xs font-semibold text-ink-soft">Category</span>
                <select value={form.categoryId} onChange={(e) => { set({ categoryId: e.target.value }); clear("categoryId"); }} className={`${input} ${errors.categoryId ? "border-sale" : ""}`}>
                  <option value="">Choose…</option>
                  {flat.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
                </select>
                {err("categoryId")}
              </label>
              {text("Web address", "slug", { placeholder: slugify(form.name) || "amrapali-mango", hint: `/p/${form.slug || slugify(form.name) || "…"}` })}
              <label className="block sm:col-span-2">
                <span className="mb-1 block text-xs font-semibold text-ink-soft">Description</span>
                <textarea rows={3} value={form.description} onChange={(e) => set({ description: e.target.value })} className={input} placeholder="What it is, why people love it, what it needs." />
              </label>
              <label className="block sm:col-span-2">
                <span className="mb-1 block text-xs font-semibold text-ink-soft">Good to know</span>
                <textarea rows={3} value={form.details} onChange={(e) => set({ details: e.target.value })} className={input} placeholder={"One point per line\nGrafted, fruits in 2–3 years"} />
                <span className="mt-1 block text-xs text-muted">One point per line; shown as a list on the plant&rsquo;s page.</span>
              </label>
            </div>
          </Card>

          <Card title="Care facts" action={<span className="text-xs text-muted">Shoppers filter by these</span>}>
            <div className="grid gap-4 md:grid-cols-2">
              {choice("Light", "light", lightLabels)}
              {choice("Water", "water", waterLabels)}
              {choice("Care", "careLevel", careLabels)}
              {choice("Where it grows", "placement", placementLabels)}
              {choice("Growth", "growthRate", { ...growthLabels, "": "Not set" } as Record<Growth | "", string>)}
              <label className="flex items-center gap-2 self-end text-sm font-semibold"><input type="checkbox" checked={form.petFriendly} onChange={(e) => set({ petFriendly: e.target.checked })} className="h-4 w-4 accent-forest" /> Safe for cats and dogs</label>
              {text("Grows to", "matureHeight", { placeholder: "e.g. 4–6 m" })}
              {text("Fruiting (fruit plants only)", "fruiting", { placeholder: "e.g. Fruits in 2–3 years" })}
            </div>
          </Card>

          <Card title="Planters" action={<span className="text-xs text-muted">Each planter has its own pictures and prices</span>}>
            {err("planters")}
            <ul className="space-y-2">
              {form.planters.map((p, i) => (
                <li key={p.key} className="flex flex-wrap items-start gap-2 rounded-2xl bg-bone p-2.5">
                  <label className="relative h-10 w-10 shrink-0 cursor-pointer overflow-hidden rounded-full ring-1 ring-line" style={{ background: p.hex }} title="Colour">
                    <span className="sr-only">Colour of {p.name}</span>
                    <input type="color" value={p.hex} onChange={(e) => setPlanter(i, { hex: e.target.value.toUpperCase() })} className="absolute inset-0 cursor-pointer opacity-0" />
                  </label>
                  <label className="min-w-40 flex-1">
                    <span className="sr-only">Planter name</span>
                    <input value={p.name} onChange={(e) => setPlanter(i, { name: e.target.value })} className={`${input} ${errors[`planters.${i}.name`] ? "border-sale" : ""}`} />
                    {err(`planters.${i}.name`)}
                  </label>
                  <select aria-label="Kind" value={p.kind} onChange={(e) => setPlanter(i, { kind: e.target.value as PlanterKind })} className={`${input} w-auto!`}>
                    {KINDS.map((k) => <option key={k.value} value={k.value}>{k.label}</option>)}
                  </select>
                  <span className="flex gap-1">
                    <button type="button" aria-label="Move up" disabled={i === 0} onClick={() => movePlanter(i, -1)} className={btn.icon}><ArrowUp size={15} /></button>
                    <button type="button" aria-label="Move down" disabled={i === form.planters.length - 1} onClick={() => movePlanter(i, 1)} className={btn.icon}><ArrowDown size={15} /></button>
                    <button type="button" aria-label={`Remove ${p.name}`} disabled={form.planters.length === 1} onClick={() => removePlanter(i)} className={btn.icon}><Trash2 size={15} /></button>
                  </span>
                </li>
              ))}
            </ul>
            <div className="mt-3 flex flex-wrap gap-2">
              {KINDS.map((k) => <button key={k.value} type="button" onClick={() => addPlanter(k.value)} className={`${btn.ghost} px-3 py-1.5 text-xs`}><Plus size={14} /> {k.label}</button>)}
            </div>
            <p className="mt-2 text-xs text-muted">The first planter is shown first in the shop.</p>
          </Card>

          <Card title="Sizes and prices" action={<span className="text-xs text-muted">{offered} option{offered === 1 ? "" : "s"} offered</span>}>
            {err("variants")}
            <div className="space-y-4">
              {form.planters.map((p) => (
                <div key={p.key}>
                  <p className="mb-2 flex items-center gap-2 text-sm font-semibold"><span className="h-4 w-4 rounded-full ring-1 ring-line" style={{ background: p.hex }} /> {p.name}</p>
                  <div className="space-y-2">
                    {SIZES.map((s) => {
                      const key = cellKey(p.key, s.value);
                      const c = form.cells[key] ?? blankCell(s.value);
                      const e = (f: string) => errors[`${key}:${f}`];
                      const cls = (f: string) => `${input} tabular-nums ${e(f) ? "border-sale" : ""}`;
                      return (
                        <div key={s.value} className={`rounded-2xl border p-3 transition ${c.on ? "border-forest/30 bg-white" : "border-dashed border-line bg-bone/60"}`}>
                          <label className="flex items-center gap-2 text-sm font-semibold">
                            <input type="checkbox" checked={c.on} onChange={(ev) => setCell(key, { on: ev.target.checked, sku: c.sku || suggestSku(form.name, s.value, p, form.planters) })} className="h-4 w-4 accent-forest" />
                            {s.label}
                            {!c.on && <span className="font-normal text-muted">· not offered</span>}
                            {c.on && c.price && <span className="ml-auto font-normal text-muted">{taka(Number(c.price) || 0)}{Number(c.stock) <= 0 && " · sold out"}</span>}
                          </label>
                          {c.on && (
                            <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
                              <label className="block"><span className="mb-1 block text-[0.7rem] font-semibold text-muted">Height</span><input value={c.height} onChange={(ev) => setCell(key, { height: ev.target.value })} className={cls("height")} /></label>
                              <label className="block"><span className="mb-1 block text-[0.7rem] font-semibold text-muted">Price ৳</span><input inputMode="numeric" value={c.price} onChange={(ev) => setCell(key, { price: ev.target.value })} className={cls("price")} /></label>
                              <label className="block"><span className="mb-1 block text-[0.7rem] font-semibold text-muted">Old price ৳</span><input inputMode="numeric" value={c.compareAtPrice} onChange={(ev) => setCell(key, { compareAtPrice: ev.target.value })} placeholder="—" className={cls("compareAtPrice")} /></label>
                              <label className="block"><span className="mb-1 block text-[0.7rem] font-semibold text-muted">In stock</span><input inputMode="numeric" value={c.stock} onChange={(ev) => setCell(key, { stock: ev.target.value })} className={cls("stock")} /></label>
                              <label className="col-span-2 block"><span className="mb-1 block text-[0.7rem] font-semibold text-muted">SKU</span><input value={c.sku} onChange={(ev) => setCell(key, { sku: ev.target.value.toUpperCase() })} className={`${cls("sku")} font-mono uppercase`} /></label>
                              <label className="col-span-2 flex items-center gap-2 text-xs font-semibold sm:col-span-3 lg:col-span-6"><input type="checkbox" checked={c.dhakaOnly} onChange={(ev) => setCell(key, { dhakaOnly: ev.target.checked })} className="h-4 w-4 accent-forest" /> <Truck size={14} className="text-clay" /> Inside Dhaka only (too big for a courier)</label>
                              {["height", "price", "compareAtPrice", "stock", "sku", "size", "planter"].map((f) => e(f) && <span key={f} className="col-span-full text-xs font-semibold text-sale">{e(f)}</span>)}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {product ? <Pictures product={product} onChange={(p) => { setProduct(p); }} /> : (
            <Card title="Pictures"><p className="text-sm text-muted">Save the plant first, then add pictures.</p></Card>
          )}
        </div>

        <div className="space-y-4">
          <Card title="Status">
            <div className="space-y-1.5">
              {STATUSES.map((s) => (
                <label key={s.value} className={`flex cursor-pointer items-center gap-3 rounded-2xl p-2.5 ring-1 transition ${form.status === s.value ? "bg-sage/40 ring-forest/40" : "ring-line hover:ring-forest/30"}`}>
                  <input type="radio" name="status" checked={form.status === s.value} onChange={() => { set({ status: s.value }); clear("status"); }} className="h-4 w-4 accent-forest" />
                  <span><span className="block text-sm font-semibold">{s.label}</span><span className="text-xs text-muted">{s.note}</span></span>
                </label>
              ))}
            </div>
            <label className="mt-3 flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={form.featured} onChange={(e) => set({ featured: e.target.checked })} className="h-4 w-4 accent-forest" /> Feature on the home page</label>
          </Card>
          <Card title="Collections">
            {collections.length === 0 ? <p className="text-sm text-muted">No collections yet.</p> : (
              <div className="flex flex-wrap gap-1.5">
                {collections.map((c) => {
                  const on = form.collectionIds.includes(c.id);
                  return (
                    <button key={c.id} type="button" aria-pressed={on} onClick={() => set({ collectionIds: on ? form.collectionIds.filter((x) => x !== c.id) : [...form.collectionIds, c.id] })} className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${on ? "bg-forest text-bone" : "bg-white text-ink-soft ring-1 ring-line hover:ring-forest/50"}`}>
                      {c.name}{!c.active && " (hidden)"}
                    </button>
                  );
                })}
              </div>
            )}
          </Card>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-white/95 backdrop-blur lg:left-[15.5rem]">
        <div className="flex items-center justify-end gap-3 px-4 py-3 md:px-8">
          <span className="mr-auto text-sm text-muted">{dirty ? "Unsaved changes" : id === null ? "New plant" : "All changes saved"}</span>
          {dirty && id !== null && product && <button type="button" onClick={() => { setForm(toForm(product)); setDirty(false); setErrors({}); }} className={btn.ghost}>Discard</button>}
          <button type="button" disabled={busy} onClick={save} className={btn.primary}>{busy ? "Saving…" : id === null ? "Create plant" : "Save changes"}</button>
        </div>
      </div>
    </div>
  );
}

/** Pictures save at once (they don't wait for the save button). The first one is on cards. */
function Pictures({ product, onChange }: { product: AdminProduct; onChange: (p: AdminProduct) => void }) {
  const { notify } = useAdmin();
  const file = useRef<HTMLInputElement>(null);
  const [planterId, setPlanterId] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const planterName = useMemo(() => new Map(product.planters.map((p) => [p.id, p.name])), [product.planters]);

  const run = async (work: () => Promise<AdminProduct>, done: string) => {
    setBusy(true);
    try {
      onChange(await work());
      notify(done);
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  };

  const upload = async (files: FileList | null) => {
    if (!files?.length) return;
    const query = new URLSearchParams({ alt: product.name });
    if (planterId) query.set("planterId", planterId);
    for (const f of Array.from(files)) {
      await run(() => send<AdminProduct>(`/admin/products/${product.id}/images?${query}`, f), "Picture added");
    }
    if (file.current) file.current.value = "";
  };

  const patch = (img: AdminImage, changes: Partial<AdminImage>) =>
    run(() => request<AdminProduct>("PATCH", `/admin/products/${product.id}/images/${img.id}`, { planterId: img.planterId, alt: img.alt, ...changes }), "Picture updated");

  return (
    <Card title="Pictures" action={<span className="text-xs text-muted">Saved as you go</span>}>
      {product.images.length === 0 && <p className="mb-3 text-sm text-muted">No pictures yet. Plants with clear, bright photos sell far better.</p>}
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {product.images.map((img, i) => (
          <li key={img.id} className="overflow-hidden rounded-2xl border border-line bg-bone">
            <div className="relative aspect-square bg-mist">
              <img src={img.url} alt={img.alt} className="h-full w-full object-cover" />
              {i === 0 && <span className="kicker absolute left-2 top-2 rounded-full bg-lime px-2 py-0.5 text-[0.6rem] text-forest-deep">On cards</span>}
            </div>
            <div className="space-y-1.5 p-2">
              <select aria-label="Shown with" value={img.planterId ?? ""} disabled={busy} onChange={(e) => patch(img, { planterId: e.target.value ? Number(e.target.value) : null })} className={`${input} py-1.5 text-xs`}>
                <option value="">Every planter</option>
                {product.planters.map((p) => <option key={p.id} value={p.id}>{p.name} only</option>)}
              </select>
              <div className="flex justify-between gap-1">
                <button type="button" aria-label="Earlier" disabled={busy || i === 0} onClick={() => run(() => request<AdminProduct>("POST", `/admin/products/${product.id}/images/${img.id}/move`, { direction: -1 }), "Moved")} className={btn.icon}><ArrowUp size={14} className="-rotate-90" /></button>
                <button type="button" aria-label="Later" disabled={busy || i === product.images.length - 1} onClick={() => run(() => request<AdminProduct>("POST", `/admin/products/${product.id}/images/${img.id}/move`, { direction: 1 }), "Moved")} className={btn.icon}><ArrowDown size={14} className="-rotate-90" /></button>
                <button type="button" aria-label="Remove picture" disabled={busy} onClick={() => window.confirm("Remove this picture?") && run(() => request<AdminProduct>("DELETE", `/admin/products/${product.id}/images/${img.id}`), "Picture removed")} className={btn.icon}><X size={14} /></button>
              </div>
              {img.planterId && !planterName.has(img.planterId) && <p className="text-xs text-sale">Planter removed</p>}
            </div>
          </li>
        ))}
      </ul>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <select aria-label="New pictures show with" value={planterId} onChange={(e) => setPlanterId(e.target.value)} className={`${input} w-auto!`}>
          <option value="">For every planter</option>
          {product.planters.map((p) => <option key={p.id} value={p.id}>For {p.name} only</option>)}
        </select>
        <button type="button" disabled={busy} onClick={() => file.current?.click()} className={btn.lime}><ImagePlus size={16} /> {busy ? "Uploading…" : "Add pictures"}</button>
        <input ref={file} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={(e) => upload(e.target.files)} />
        <span className="text-xs text-muted">JPEG, PNG or WebP, up to 5 MB.</span>
      </div>
    </Card>
  );
}
