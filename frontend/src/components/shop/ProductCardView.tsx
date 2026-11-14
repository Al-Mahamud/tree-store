import Link from "next/link";
import { PawPrint } from "lucide-react";
import type { ProductCard } from "@/lib/types";
import { careLabels, lightLabels } from "@/lib/care";
import { percentOff } from "@/lib/money";
import { Price } from "@/components/ui/Price";
import { LightIcon } from "@/components/ui/CareIcon";
import { RatingBadge } from "@/components/ui/Stars";
import { SaveButton } from "@/components/account/SaveButton";

/* eslint-disable @next/next/no-img-element -- plant images are served by the backend as-is. */
export function ProductCardView({ product, priority = false }: { product: ProductCard; priority?: boolean }) {
  const off = percentOff(product.fromPrice, product.compareAtPrice);
  return (
    <article className="group">
      <Link href={`/p/${product.slug}`} className="block">
        <div className="relative aspect-[4/5] overflow-hidden rounded-[var(--radius-card)] bg-mist">
          {product.image && (
            <img
              src={product.image}
              alt={product.name}
              loading={priority ? "eager" : "lazy"}
              className="absolute inset-0 h-full w-full object-cover transition duration-700 ease-out-soft group-hover:scale-[1.04]"
            />
          )}
          {product.hoverImage && (
            <img src={product.hoverImage} alt="" aria-hidden loading="lazy" className="absolute inset-0 h-full w-full object-cover opacity-0 transition duration-500 group-hover:opacity-100" />
          )}
          <div className="absolute right-3 top-3"><SaveButton productId={product.id} name={product.name} /></div>
          <div className="absolute left-3 top-3 flex flex-wrap gap-1.5">
            {product.soldOut ? (
              <Pill tone="dark">Sold out</Pill>
            ) : off ? (
              <Pill tone="sale">−{off}%</Pill>
            ) : null}
            {product.isNew && !product.soldOut && <Pill tone="lime">New</Pill>}
          </div>
          <div className="absolute inset-x-3 bottom-3 flex flex-wrap gap-1.5 opacity-100 transition duration-300 md:translate-y-1 md:opacity-0 md:group-hover:translate-y-0 md:group-hover:opacity-100">
            <Pill tone="glass">
              <LightIcon light={product.light} size={13} /> {lightLabels[product.light]}
            </Pill>
            <Pill tone="glass">{careLabels[product.careLevel]}</Pill>
            {product.petFriendly && (
              <Pill tone="glass">
                <PawPrint size={13} strokeWidth={1.7} /> Pet-safe
              </Pill>
            )}
          </div>
        </div>
        <div className="mt-4 flex items-start justify-between gap-3 px-1">
          <div className="min-w-0">
            <h3 className="font-display text-[1.375rem] leading-tight">{product.name}</h3>
            <p className="mt-0.5 truncate text-xs text-muted">
              {product.localName && <span className="font-bangla">{product.localName}</span>}
              {product.localName && " · "}
              {product.fruiting ?? product.category}
            </p>
          </div>
          <div className="shrink-0 text-right">
            <Price price={product.fromPrice} compareAt={product.compareAtPrice} from={product.priceVaries} />
          </div>
        </div>
      </Link>
      {(product.planters.length > 1 || product.reviewCount > 0) && (
        <div className="mt-2 flex items-center justify-between gap-3 px-1">
          {product.planters.length > 1 ? (
            <div className="flex items-center gap-1.5" aria-label={`${product.planters.length} planters`}>
              {product.planters.map((p) => (
                <span key={p.name} title={p.name} className="h-3.5 w-3.5 rounded-full ring-1 ring-black/10" style={{ backgroundColor: p.hex }} />
              ))}
            </div>
          ) : <span />}
          <RatingBadge rating={product.rating} count={product.reviewCount} />
        </div>
      )}
    </article>
  );
}

function Pill({ tone, children }: { tone: "dark" | "sale" | "lime" | "glass"; children: React.ReactNode }) {
  const tones = {
    dark: "bg-forest-deep text-bone",
    sale: "bg-sale text-white",
    lime: "bg-lime text-forest-deep",
    glass: "bg-bone/85 text-ink backdrop-blur",
  };
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[0.6875rem] font-semibold ${tones[tone]}`}>
      {children}
    </span>
  );
}
