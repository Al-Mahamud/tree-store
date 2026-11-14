"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BadgeCheck, Camera, MessageSquareQuote, PenLine } from "lucide-react";
import type { MyReview, Review, ReviewForProduct, ReviewPage, ReviewSort } from "@/lib/types";
import { request } from "@/lib/client";
import { longDate } from "@/lib/format";
import { site } from "@/config/site";
import { useAccount } from "@/components/account/AccountProvider";
import { Modal } from "@/components/ui/Modal";
import { Stars } from "@/components/ui/Stars";
import { ReviewForm } from "./ReviewForm";

const SORTS: { value: ReviewSort; label: string }[] = [
  { value: "RECENT", label: "Newest" },
  { value: "PHOTOS", label: "With photos" },
  { value: "HIGHEST", label: "Highest" },
  { value: "LOWEST", label: "Lowest" },
];

/** Reviews on a product page: summary, the shopper's own review, and the list (6 at a time). */
/* eslint-disable @next/next/no-img-element -- review photos are served by the backend as-is. */
export function Reviews({ slug, product, initial }: { slug: string; product: string; initial: ReviewPage }) {
  const { me } = useAccount();
  const [summary, setSummary] = useState(initial.summary);
  const [items, setItems] = useState(initial.items);
  const [total, setTotal] = useState(initial.total);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<ReviewSort>("RECENT");
  const [loading, setLoading] = useState(false);
  const [mine, setMine] = useState<ReviewForProduct | null>(null);
  const [writing, setWriting] = useState(false);
  const [photo, setPhoto] = useState<string | null>(null);

  useEffect(() => {
    if (!me) return;
    let live = true;
    request<ReviewForProduct>("GET", `/account/reviews/for/${encodeURIComponent(slug)}`)
      .then((r) => live && setMine(r))
      .catch(() => live && setMine(null));
    return () => {
      live = false;
    };
  }, [me, slug]);

  const load = async (nextSort: ReviewSort, nextPage: number) => {
    setLoading(true);
    try {
      const r = await request<ReviewPage>("GET", `/products/${encodeURIComponent(slug)}/reviews?sort=${nextSort}&page=${nextPage}`);
      setSummary(r.summary);
      setTotal(r.total);
      setItems((old) => (nextPage === 0 ? r.items : [...old, ...r.items]));
      setPage(nextPage);
      setSort(nextSort);
    } finally {
      setLoading(false);
    }
  };

  const saved = (review: MyReview | null) => {
    setWriting(false);
    setMine({ canReview: true, review });
    void load(sort, 0);
  };

  const own = mine?.review ?? null;
  return (
    <section id="reviews" className="scroll-mt-24" aria-labelledby="reviews-heading">
      <div className="grid gap-10 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)] lg:gap-16">
        <div className="lg:sticky lg:top-24 lg:self-start">
          <p className="kicker text-moss">From people who grew it</p>
          <h2 id="reviews-heading" className="mt-2 font-display text-4xl md:text-5xl">Reviews</h2>
          {summary.count > 0 && summary.average !== null ? (
            <div className="mt-6">
              <div className="flex items-end gap-3">
                <span className="font-display text-6xl leading-none">{summary.average.toFixed(1)}</span>
                <div className="pb-1">
                  <Stars value={summary.average} size={18} />
                  <p className="mt-1 text-xs text-muted">{summary.count} {summary.count === 1 ? "review" : "reviews"}</p>
                </div>
              </div>
              <ul className="mt-5 space-y-1.5" aria-label="Reviews by stars">
                {summary.stars.map((n, i) => (
                  <li key={i} className="flex items-center gap-3 text-xs">
                    <span className="w-3 tabular-nums text-muted">{5 - i}</span>
                    <span className="h-2 flex-1 overflow-hidden rounded-full bg-mist">
                      <span className="block h-full rounded-full bg-sun" style={{ width: `${summary.count ? (n / summary.count) * 100 : 0}%` }} />
                    </span>
                    <span className="w-6 text-right tabular-nums text-muted">{n}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="mt-4 text-sm text-muted">No reviews yet.</p>
          )}

          <div className="mt-7 rounded-[1.5rem] bg-mist p-5 text-sm">
            {me === null ? (
              <>
                <p className="font-semibold">Grown this plant?</p>
                <p className="mt-1 text-muted">Reviews come from customers whose plant was delivered.</p>
                <Link href={`/login?next=${encodeURIComponent(`/p/${slug}#reviews`)}`} className="mt-3 inline-flex rounded-full bg-forest px-5 py-2.5 text-sm font-semibold text-bone">Sign in to review</Link>
              </>
            ) : mine?.canReview ? (
              <>
                <p className="font-semibold">{own ? "Your review" : "You grew this one"}</p>
                {own ? (
                  <div className="mt-2 flex items-center gap-2">
                    <Stars value={own.rating} />
                    {own.status === "HIDDEN" && <span className="rounded-full bg-clay/10 px-2 py-0.5 text-[0.6875rem] font-semibold text-clay">Hidden by the shop</span>}
                  </div>
                ) : (
                  <p className="mt-1 text-muted">Tell other growers how it arrived and settled in.</p>
                )}
                <button type="button" onClick={() => setWriting(true)} className="mt-3 inline-flex items-center gap-2 rounded-full bg-forest px-5 py-2.5 text-sm font-semibold text-bone">
                  <PenLine size={15} /> {own ? "Edit your review" : "Write a review"}
                </button>
              </>
            ) : (
              <p className="text-muted">Reviews come from customers whose {product} was delivered. Every one is from a real order.</p>
            )}
          </div>
        </div>

        <div>
          {summary.count > 0 && (
            <div className="flex flex-wrap gap-2" role="group" aria-label="Sort reviews">
              {SORTS.filter((s) => s.value !== "PHOTOS" || summary.withPhotos > 0).map((s) => (
                <button
                  key={s.value}
                  type="button"
                  aria-pressed={sort === s.value}
                  onClick={() => sort !== s.value && load(s.value, 0)}
                  className={`inline-flex items-center gap-1.5 rounded-full border px-4 py-2 text-sm transition ${sort === s.value ? "border-forest bg-forest font-semibold text-bone" : "border-line hover:border-forest"}`}
                >
                  {s.value === "PHOTOS" && <Camera size={14} />}{s.label}
                </button>
              ))}
            </div>
          )}

          {items.length === 0 ? (
            <div className="mt-2 rounded-[var(--radius-card)] border border-dashed border-line p-10 text-center">
              <MessageSquareQuote size={28} className="mx-auto text-moss" strokeWidth={1.5} />
              <p className="mt-3 font-display text-2xl">Be the first to review it</p>
              <p className="mt-1 text-sm text-muted">Once your {product} has settled in, tell others how it is growing.</p>
            </div>
          ) : (
            <ol className={`mt-6 divide-y divide-line border-y border-line transition ${loading && page === 0 ? "opacity-50" : ""}`}>
              {items.map((r) => <ReviewItem key={r.id} review={r} onPhoto={setPhoto} />)}
            </ol>
          )}

          {items.length < total && (
            <button type="button" disabled={loading} onClick={() => load(sort, page + 1)} className="mt-6 rounded-full border border-forest px-6 py-3 text-sm font-semibold transition hover:bg-forest hover:text-bone disabled:opacity-50">
              {loading ? "Loading…" : `Show more reviews (${total - items.length})`}
            </button>
          )}
        </div>
      </div>

      <Modal open={writing} onClose={() => setWriting(false)} title={own ? "Edit your review" : "Write a review"}>
        {writing && <ReviewForm slug={slug} product={product} existing={own} onDone={saved} />}
      </Modal>
      <Modal open={photo !== null} onClose={() => setPhoto(null)} title="Photo from a customer">
        {photo && <img src={photo} alt="Customer's photo of the plant" className="mx-auto max-h-[60vh] rounded-2xl object-contain" />}
      </Modal>
    </section>
  );
}

function ReviewItem({ review, onPhoto }: { review: Review; onPhoto: (url: string) => void }) {
  return (
    <li className="py-7">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <Stars value={review.rating} />
        {review.title && <h3 className="font-semibold">{review.title}</h3>}
      </div>
      <p className="mt-3 whitespace-pre-line text-[0.9375rem] leading-relaxed text-ink-soft">{review.body}</p>
      {review.photos.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {review.photos.map((p) => (
            <button key={p.id} type="button" onClick={() => onPhoto(p.url)} className="overflow-hidden rounded-2xl ring-offset-2 transition hover:ring-2 hover:ring-forest" aria-label="Open photo">
              <img src={p.url} alt="" loading="lazy" className="h-24 w-24 object-cover" />
            </button>
          ))}
        </div>
      )}
      <p className="mt-4 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted">
        <span className="font-semibold text-ink">{review.author}</span>
        <span className="inline-flex items-center gap-1 text-ok"><BadgeCheck size={14} /> Verified buyer</span>
        {review.bought && <span>· {review.bought}</span>}
        <span>· {longDate(review.createdAt)}{review.edited ? " (edited)" : ""}</span>
      </p>
      {review.reply && (
        <div className="mt-4 rounded-[1.25rem] bg-mist p-4 text-sm">
          <p className="text-xs font-semibold text-moss">Reply from {site.name}</p>
          <p className="mt-1 whitespace-pre-line text-ink-soft">{review.reply}</p>
        </div>
      )}
    </li>
  );
}
