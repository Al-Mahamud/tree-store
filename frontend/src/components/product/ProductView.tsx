"use client";

import { useState } from "react";
import Link from "next/link";
import { Heart, MapPin, PawPrint, Ruler, ShieldCheck, Sprout, TrendingUp, Apple, Home as HomeIcon, Truck } from "lucide-react";
import type { ProductDetail } from "@/lib/types";
import { careLabels, growthLabels, lightLabels, placementLabels, waterLabels } from "@/lib/care";
import { site } from "@/config/site";
import { taka } from "@/lib/money";
import { Price } from "@/components/ui/Price";
import { CareLevelIcon, LightIcon, WaterIcon } from "@/components/ui/CareIcon";
import { Gallery } from "./Gallery";
import { AddToBag } from "./AddToBag";

const LOW_STOCK = 3;

export function ProductView({ product, initialPlanter }: { product: ProductDetail; initialPlanter?: string }) {
  const start = product.planters.find((p) => p.name.toLowerCase() === initialPlanter?.toLowerCase()) ?? product.planters[0];
  const [planterId, setPlanterId] = useState(start?.id);
  const planter = product.planters.find((p) => p.id === planterId) ?? product.planters[0];
  const firstInStock = planter?.options.find((o) => o.stock > 0) ?? planter?.options[0];
  const [size, setSize] = useState(firstInStock?.size ?? null);
  const option = planter?.options.find((o) => o.size === size) ?? null;

  const choosePlanter = (id: number) => {
    setPlanterId(id);
    const next = product.planters.find((p) => p.id === id);
    if (next && !next.options.some((o) => o.size === size && o.stock > 0)) {
      setSize(next.options.find((o) => o.stock > 0)?.size ?? next.options[0]?.size ?? null);
    }
  };

  const facts = [
    { icon: <LightIcon light={product.care.light} size={18} />, label: "Light", value: lightLabels[product.care.light] },
    { icon: <WaterIcon water={product.care.water} size={18} />, label: "Water", value: waterLabels[product.care.water] },
    { icon: <CareLevelIcon level={product.care.careLevel} size={18} />, label: "Care", value: careLabels[product.care.careLevel] },
    { icon: <HomeIcon size={18} strokeWidth={1.7} />, label: "Grows", value: placementLabels[product.care.placement] },
    product.care.matureHeight && { icon: <Ruler size={18} strokeWidth={1.7} />, label: "Mature height", value: product.care.matureHeight },
    product.care.growthRate && { icon: <TrendingUp size={18} strokeWidth={1.7} />, label: "Growth", value: growthLabels[product.care.growthRate] },
    product.care.fruiting && { icon: <Apple size={18} strokeWidth={1.7} />, label: "Fruiting", value: product.care.fruiting.replace("Fruits in ", "") },
    { icon: <PawPrint size={18} strokeWidth={1.7} />, label: "Pets", value: product.care.petFriendly ? "Safe for pets" : "Keep away from pets" },
  ].filter(Boolean) as { icon: React.ReactNode; label: string; value: string }[];

  return (
    <div className="grid gap-8 lg:grid-cols-2 lg:gap-14 xl:gap-20">
      <div className="lg:sticky lg:top-24 lg:self-start">
        <Gallery key={planter?.id} images={planter?.images ?? []} name={product.name} />
      </div>

      <div>
        <nav aria-label="Breadcrumb" className="text-xs text-muted">
          <ol className="flex flex-wrap items-center gap-1.5">
            <li><Link href="/" className="hover:text-ink">Home</Link></li>
            {product.breadcrumb.map((c) => (
              <li key={c.slug} className="flex items-center gap-1.5"><span aria-hidden>/</span><Link href={`/c/${c.slug}`} className="hover:text-ink">{c.name}</Link></li>
            ))}
          </ol>
        </nav>

        <div className="mt-6 flex flex-wrap items-center gap-2">
          {product.localName && <span className="rounded-full bg-mist px-3 py-1 font-bangla text-sm">{product.localName}</span>}
          {product.isNew && <span className="rounded-full bg-lime px-3 py-1 text-xs font-semibold">New arrival</span>}
        </div>
        <h1 className="mt-4 font-display text-5xl leading-[0.95] md:text-6xl">{product.name}</h1>
        {product.botanicalName && <p className="mt-2 font-display text-xl italic text-muted">{product.botanicalName}</p>}
        <div className="mt-5">
          {option ? <Price price={option.price} compareAt={option.compareAtPrice} size="lg" /> : null}
        </div>

        {planter && (
          <fieldset className="mt-8">
            <legend className="text-sm"><span className="text-muted">Size:</span> <span className="font-semibold">{option ? `${option.sizeLabel}, ${option.height}` : "Choose a size"}</span></legend>
            <div className="mt-3 grid grid-cols-3 gap-2">
              {planter.options.map((o) => {
                const out = o.stock === 0;
                const selected = o.size === size;
                return (
                  <button
                    key={o.variantId}
                    type="button"
                    disabled={out}
                    aria-pressed={selected}
                    onClick={() => setSize(o.size)}
                    className={`relative rounded-[1.25rem] border p-3 text-left transition ${
                      selected ? "border-forest bg-forest text-bone shadow-lg shadow-forest/20" : out ? "cursor-not-allowed border-line text-muted/60" : "border-line bg-white/60 hover:border-forest"
                    }`}
                  >
                    <Sprout size={o.size === "SMALL" ? 14 : o.size === "MEDIUM" ? 18 : 22} strokeWidth={1.6} className={selected ? "text-lime" : "text-moss"} />
                    <span className="mt-2 block text-sm font-semibold">{o.sizeLabel}</span>
                    <span className={`block text-xs ${selected ? "text-bone/70" : "text-muted"}`}>{o.height}</span>
                    <span className="mt-1.5 block text-sm font-semibold tabular-nums">{out ? "Sold out" : taka(o.price)}</span>
                    {o.dhakaOnly && !out && <span className={`absolute right-2 top-2 rounded-full px-1.5 py-0.5 text-[0.5625rem] font-bold uppercase ${selected ? "bg-lime text-forest-deep" : "bg-clay/10 text-clay"}`}>Dhaka</span>}
                  </button>
                );
              })}
            </div>
          </fieldset>
        )}

        {product.planters.length > 0 && (
          <fieldset className="mt-6">
            <legend className="text-sm"><span className="text-muted">Planter:</span> <span className="font-semibold">{planter?.name}</span></legend>
            <div className="mt-3 flex flex-wrap gap-2">
              {product.planters.map((p) => (
                <button key={p.id} type="button" aria-pressed={p.id === planter?.id} onClick={() => choosePlanter(p.id)} className={`inline-flex items-center gap-2 rounded-full border py-1.5 pl-1.5 pr-4 text-sm transition ${p.id === planter?.id ? "border-forest bg-white font-semibold" : "border-line hover:border-forest"}`}>
                  <span className="h-7 w-7 rounded-full ring-1 ring-black/10" style={{ backgroundColor: p.hex }} />
                  {p.name}
                </button>
              ))}
            </div>
          </fieldset>
        )}

        <div className="mt-8 flex gap-2">
          <AddToBag variantId={option && option.stock > 0 ? option.variantId : null} price={option?.price ?? null} soldOut={!option || option.stock === 0} />
          <button type="button" aria-label="Save to wishlist" className="grid h-14 w-14 shrink-0 place-items-center rounded-full border border-line transition hover:border-forest">
            <Heart size={20} strokeWidth={1.7} />
          </button>
        </div>
        <p className="mt-3 min-h-5 text-center text-xs" role="status">
          {option && option.stock > 0 && option.stock <= LOW_STOCK ? <span className="font-semibold text-clay">Only {option.stock} left in this size and planter</span>
            : option && option.stock > 0 ? <span className="text-ok">Ready to dispatch from our nursery</span> : null}
        </p>

        {option?.dhakaOnly && (
          <p className="mt-4 flex gap-3 rounded-[1.25rem] bg-clay/10 p-4 text-sm text-ink-soft">
            <MapPin size={18} className="mt-0.5 shrink-0 text-clay" />
            <span><strong className="font-semibold text-ink">Delivered inside Dhaka only.</strong> Trees this size travel by our own van. Outside Dhaka? Choose Small or Medium.</span>
          </p>
        )}

        <div className="mt-8 grid grid-cols-2 gap-2 sm:grid-cols-4">
          {facts.map((f) => (
            <div key={f.label} className="rounded-[1.25rem] bg-mist p-3.5">
              <span className="text-moss">{f.icon}</span>
              <p className="mt-2.5 text-[0.6875rem] font-semibold uppercase tracking-wide text-muted">{f.label}</p>
              <p className="mt-0.5 text-sm font-semibold leading-snug">{f.value}</p>
            </div>
          ))}
        </div>

        <div className="mt-8 divide-y divide-line border-y border-line">
          <Accordion title="About this plant" defaultOpen><p>{product.description}</p></Accordion>
          {product.details.length > 0 && (
            <Accordion title="Why you'll love it">
              <ul className="space-y-2">
                {product.details.map((d) => <li key={d} className="flex gap-2"><Sprout size={15} className="mt-0.5 shrink-0 text-moss" />{d}</li>)}
              </ul>
            </Accordion>
          )}
          <Accordion title="Delivery & guarantee">
            <ul className="space-y-2">
              <li className="flex gap-2"><Truck size={15} className="mt-0.5 shrink-0 text-moss" />Inside Dhaka 1–2 days ({taka(site.deliveryFees.insideDhaka)}), outside Dhaka 2–4 days ({taka(site.deliveryFees.outsideDhaka)}). Free over {taka(site.freeDeliveryOver)}.</li>
              <li className="flex gap-2"><ShieldCheck size={15} className="mt-0.5 shrink-0 text-moss" />{site.guaranteeDays}-day healthy-plant guarantee: if it arrives unwell or fails to settle, send us a photo and we replace it.</li>
            </ul>
          </Accordion>
        </div>
      </div>
    </div>
  );
}

function Accordion({ title, defaultOpen = false, children }: { title: string; defaultOpen?: boolean; children: React.ReactNode }) {
  return (
    <details open={defaultOpen} className="group">
      <summary className="flex cursor-pointer list-none items-center justify-between py-4 font-display text-xl [&::-webkit-details-marker]:hidden">
        {title}
        <span aria-hidden className="grid h-7 w-7 place-items-center rounded-full bg-mist text-base transition group-open:rotate-45">+</span>
      </summary>
      <div className="pb-5 text-sm leading-relaxed text-ink-soft">{children}</div>
    </details>
  );
}
