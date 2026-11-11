"use client";

import Link from "next/link";
import { useState } from "react";
import { Minus, Plus, Trash2 } from "lucide-react";
import type { CartLine } from "@/lib/types";
import { taka } from "@/lib/money";
import { ApiError } from "@/lib/client";
import { useCart } from "./CartProvider";

const problems = {
  UNAVAILABLE: "No longer available — please remove it.",
  OUT_OF_STOCK: "Sold out — please remove it.",
  NOT_ENOUGH_STOCK: "Fewer left than you chose — please lower the quantity.",
} as const;

/* eslint-disable @next/next/no-img-element */
export function CartLines({ lines, compact = false }: { lines: CartLine[]; compact?: boolean }) {
  return (
    <ul className="space-y-3">
      {lines.map((line) => <Line key={line.variantId} line={line} compact={compact} />)}
    </ul>
  );
}

function Line({ line, compact }: { line: CartLine; compact: boolean }) {
  const { setQuantity, remove, setOpen } = useCart();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const change = async (action: () => Promise<void>) => {
    setPending(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Something went wrong.");
    } finally {
      setPending(false);
    }
  };
  const max = Math.min(10, line.stock);
  return (
    <li className={`flex gap-4 rounded-3xl bg-white/60 p-3 transition-opacity ${pending ? "opacity-50" : ""}`}>
      <Link href={`/p/${line.slug}`} onClick={() => setOpen(false)} className={`shrink-0 overflow-hidden rounded-2xl bg-mist ${compact ? "w-20" : "w-24 md:w-28"}`}>
        <div className="aspect-[4/5]">{line.image && <img src={line.image} alt="" className="h-full w-full object-cover" />}</div>
      </Link>
      <div className="flex min-w-0 flex-1 flex-col py-1">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <Link href={`/p/${line.slug}`} onClick={() => setOpen(false)} className="font-display text-xl leading-tight hover:underline underline-offset-4">{line.name}</Link>
            <p className="mt-0.5 text-xs text-muted">{line.size} · {line.height} · {line.planter}</p>
            {line.dhakaOnly && <p className="mt-1 inline-block rounded-full bg-clay/10 px-2 py-0.5 text-[0.6875rem] font-semibold text-clay">Dhaka delivery only</p>}
          </div>
          <button type="button" aria-label={`Remove ${line.name}`} disabled={pending} onClick={() => change(() => remove(line.variantId))} className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-muted hover:bg-mist hover:text-sale">
            <Trash2 size={15} />
          </button>
        </div>
        <div className="mt-auto flex items-end justify-between gap-3 pt-3">
          <div className="flex items-center rounded-full bg-mist p-0.5">
            <button type="button" aria-label="Fewer" disabled={pending || line.quantity <= 1} onClick={() => change(() => setQuantity(line.variantId, line.quantity - 1))} className="grid h-8 w-8 place-items-center rounded-full hover:bg-white disabled:text-muted/40">
              <Minus size={14} />
            </button>
            <span className="w-7 text-center text-sm font-semibold tabular-nums" aria-live="polite">{line.quantity}</span>
            <button type="button" aria-label="More" disabled={pending || line.quantity >= max} onClick={() => change(() => setQuantity(line.variantId, line.quantity + 1))} className="grid h-8 w-8 place-items-center rounded-full hover:bg-white disabled:text-muted/40">
              <Plus size={14} />
            </button>
          </div>
          <p className="text-[0.9375rem] font-semibold tabular-nums">{taka(line.lineTotal)}</p>
        </div>
        {(line.problem || error) && <p role="alert" className="mt-2 text-xs text-sale">{error ?? (line.problem ? problems[line.problem] : null)}</p>}
      </div>
    </li>
  );
}
