"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PenLine, Star } from "lucide-react";
import type { AccountReviews, MyReview } from "@/lib/types";
import { request } from "@/lib/client";
import { longDate } from "@/lib/format";
import { site } from "@/config/site";
import { Modal } from "@/components/ui/Modal";
import { Stars } from "@/components/ui/Stars";
import { ReviewForm } from "./ReviewForm";

type Editing = { slug: string; product: string; existing: MyReview | null };

/** The account's Reviews tab: delivered plants waiting for a review, then the reviews already written. */
/* eslint-disable @next/next/no-img-element -- plant images are served by the backend as-is. */
export function MyReviews() {
  const [data, setData] = useState<AccountReviews | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);

  const load = () => request<AccountReviews>("GET", "/account/reviews").then(setData).catch(() => setData({ toReview: [], mine: [] }));
  useEffect(() => {
    void load();
  }, []);

  if (!data) return <div className="h-72 animate-pulse rounded-[2rem] bg-white/60" aria-busy="true" />;

  if (data.toReview.length === 0 && data.mine.length === 0) {
    return (
      <div className="grid place-items-center rounded-[2rem] bg-white/60 px-6 py-16 text-center">
        <span className="grid h-16 w-16 place-items-center rounded-full bg-sage text-forest"><Star size={26} /></span>
        <h2 className="mt-4 font-display text-3xl">No reviews yet</h2>
        <p className="mt-1 max-w-sm text-sm text-muted">Once a plant has been delivered to you, it shows up here so you can tell other growers how it&rsquo;s doing.</p>
        <Link href="/shop" className="mt-6 rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone">Browse plants</Link>
      </div>
    );
  }

  return (
    <div className="space-y-10">
      {data.toReview.length > 0 && (
        <section aria-labelledby="to-review">
          <h2 id="to-review" className="font-display text-3xl">How are they doing?</h2>
          <p className="mt-1 text-sm text-muted">Plants delivered to you that you haven&rsquo;t reviewed yet.</p>
          <ul className="mt-5 grid gap-3 sm:grid-cols-2">
            {data.toReview.map((t) => (
              <li key={t.slug} className="flex items-center gap-4 rounded-[1.5rem] bg-white/70 p-3 pr-4">
                <Link href={`/p/${t.slug}`} className="h-20 w-16 shrink-0 overflow-hidden rounded-2xl bg-mist">
                  {t.image && <img src={t.image} alt="" className="h-full w-full object-cover" />}
                </Link>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold">{t.product}</p>
                  <p className="text-xs text-muted">{t.bought}{t.deliveredAt ? ` · delivered ${longDate(t.deliveredAt)}` : ""}</p>
                  <button type="button" onClick={() => setEditing({ slug: t.slug, product: t.product, existing: null })} className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-forest px-4 py-2 text-xs font-semibold text-bone">
                    <PenLine size={13} /> Write a review
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {data.mine.length > 0 && (
        <section aria-labelledby="written">
          <h2 id="written" className="font-display text-3xl">Your reviews</h2>
          <ul className="mt-5 space-y-3">
            {data.mine.map((r) => (
              <li key={r.id} className="rounded-[1.5rem] bg-white/70 p-5">
                <div className="flex gap-4">
                  <Link href={`/p/${r.slug}#reviews`} className="h-16 w-14 shrink-0 overflow-hidden rounded-xl bg-mist">
                    {r.image && <img src={r.image} alt="" className="h-full w-full object-cover" />}
                  </Link>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <Link href={`/p/${r.slug}#reviews`} className="font-semibold hover:underline">{r.product}</Link>
                      <button type="button" onClick={() => setEditing({ slug: r.slug, product: r.product, existing: r })} className="inline-flex items-center gap-1.5 rounded-full border border-line px-3.5 py-1.5 text-xs font-semibold hover:border-forest">
                        <PenLine size={13} /> Edit
                      </button>
                    </div>
                    <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted">
                      <Stars value={r.rating} size={13} /> {longDate(r.createdAt)}
                      {r.status === "HIDDEN" && <span className="rounded-full bg-clay/10 px-2 py-0.5 font-semibold text-clay">Hidden by the shop</span>}
                    </div>
                    {r.title && <p className="mt-2 text-sm font-semibold">{r.title}</p>}
                    <p className="mt-1 line-clamp-3 text-sm text-ink-soft">{r.body}</p>
                    {r.photos.length > 0 && (
                      <div className="mt-3 flex gap-2">{r.photos.map((p) => <img key={p.id} src={p.url} alt="" className="h-14 w-14 rounded-xl object-cover" />)}</div>
                    )}
                    {r.reply && (
                      <p className="mt-3 rounded-xl bg-mist p-3 text-xs text-ink-soft"><span className="font-semibold text-moss">Reply from {site.name}: </span>{r.reply}</p>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing?.existing ? "Edit your review" : "Write a review"}>
        {editing && (
          <ReviewForm
            key={editing.slug}
            slug={editing.slug}
            product={editing.product}
            existing={editing.existing}
            onDone={() => {
              setEditing(null);
              void load();
            }}
          />
        )}
      </Modal>
    </div>
  );
}
