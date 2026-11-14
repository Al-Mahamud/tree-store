"use client";

import { useEffect, useMemo, useState } from "react";
import { Camera, Star, Trash2, X } from "lucide-react";
import type { MyReview } from "@/lib/types";
import { ApiError, request, upload } from "@/lib/client";
import { Field } from "@/components/ui/Field";

const MAX_PHOTOS = 3;
const WORDS = ["", "Poor", "Fair", "Good", "Great", "Love it"];

/**
 * Write or change a review. The words save first, then each new photo uploads to the saved review.
 * `onDone` gets the saved review, or null after it was deleted.
 */
/* eslint-disable @next/next/no-img-element -- review photos are served by the backend as-is. */
export function ReviewForm({ slug, product, existing, onDone }: { slug: string; product: string; existing: MyReview | null; onDone: (review: MyReview | null) => void }) {
  const [rating, setRating] = useState(existing?.rating ?? 0);
  const [hover, setHover] = useState(0);
  const [title, setTitle] = useState(existing?.title ?? "");
  const [body, setBody] = useState(existing?.body ?? "");
  const [kept, setKept] = useState(existing?.photos ?? []);
  const [removed, setRemoved] = useState<number[]>([]);
  const [files, setFiles] = useState<File[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const previews = useMemo(() => files.map((f) => URL.createObjectURL(f)), [files]);
  useEffect(() => () => previews.forEach((u) => URL.revokeObjectURL(u)), [previews]);
  const room = MAX_PHOTOS - kept.length - files.length;

  const addFiles = (list: FileList | null) => {
    if (!list) return;
    const picked = Array.from(list).filter((f) => /^image\/(jpeg|png|webp)$/.test(f.type));
    const tooBig = picked.find((f) => f.size > 5 * 1024 * 1024);
    if (tooBig) setMessage(`${tooBig.name} is over 5 MB. Choose a smaller photo.`);
    else if (picked.length < list.length) setMessage("Photos can be JPEG, PNG or WebP.");
    else setMessage(null);
    setFiles((f) => [...f, ...picked.filter((p) => p.size <= 5 * 1024 * 1024)].slice(0, MAX_PHOTOS - kept.length));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0) {
      setErrors({ rating: "Choose from 1 to 5 stars." });
      return;
    }
    setBusy(true);
    setErrors({});
    setMessage(null);
    try {
      let saved = await request<MyReview>("PUT", `/account/reviews/for/${encodeURIComponent(slug)}`, { rating, title, body });
      for (const id of removed) {
        saved = await request<MyReview>("DELETE", `/account/reviews/${saved.id}/photos/${id}`);
      }
      for (const file of files) {
        saved = await upload<MyReview>(`/account/reviews/${saved.id}/photos`, file);
      }
      onDone(saved);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fields);
        setMessage(Object.keys(err.fields).length ? null : err.message);
      } else setMessage("Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!existing || !window.confirm("Delete your review? This can't be undone.")) return;
    setBusy(true);
    try {
      await request("DELETE", `/account/reviews/${existing.id}`);
      onDone(null);
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
      setBusy(false);
    }
  };

  const shown = hover || rating;
  return (
    <form onSubmit={submit} className="space-y-5" noValidate>
      <div>
        <p id="rating-label" className="mb-2 text-xs font-semibold text-ink-soft">How is your {product} doing?</p>
        <div role="radiogroup" aria-labelledby="rating-label" className="flex items-center gap-1" onMouseLeave={() => setHover(0)}>
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              role="radio"
              aria-checked={rating === n}
              aria-label={`${n} ${n === 1 ? "star" : "stars"}: ${WORDS[n]}`}
              onClick={() => setRating(n)}
              onMouseEnter={() => setHover(n)}
              className="grid h-11 w-11 place-items-center rounded-full transition hover:bg-mist"
            >
              <Star size={30} strokeWidth={1.4} className={n <= shown ? "text-sun" : "text-line"} fill={n <= shown ? "currentColor" : "none"} />
            </button>
          ))}
          <span className="ml-2 text-sm font-semibold text-ink-soft" aria-hidden>{WORDS[shown]}</span>
        </div>
        {errors.rating && <p className="mt-1.5 text-xs text-sale">{errors.rating}</p>}
      </div>

      <Field label="Headline (optional)" error={errors.title}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={80} placeholder="Fruited in the second summer" />
      </Field>
      <Field label="Your review" error={errors.body} hint="How it arrived, how it settled in, any tips for other growers.">
        <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={5} maxLength={2000} required />
      </Field>

      <div>
        <p className="mb-2 text-xs font-semibold text-ink-soft">Photos (up to {MAX_PHOTOS})</p>
        <div className="flex flex-wrap gap-2">
          {kept.map((p) => (
            <Thumb key={p.id} src={p.url} onRemove={() => { setKept((k) => k.filter((x) => x.id !== p.id)); setRemoved((r) => [...r, p.id]); }} />
          ))}
          {previews.map((src, i) => (
            <Thumb key={src} src={src} onRemove={() => setFiles((f) => f.filter((_, j) => j !== i))} />
          ))}
          {room > 0 && (
            <label className="grid h-20 w-20 cursor-pointer place-items-center rounded-2xl border border-dashed border-moss/50 bg-white/60 text-moss transition hover:border-forest focus-within:ring-4 focus-within:ring-lime/40">
              <span className="flex flex-col items-center gap-1 text-[0.6875rem] font-semibold"><Camera size={20} strokeWidth={1.6} />Add</span>
              <input type="file" accept="image/jpeg,image/png,image/webp" multiple className="sr-only" onChange={(e) => { addFiles(e.target.files); e.target.value = ""; }} />
            </label>
          )}
        </div>
      </div>

      {message && <p role="alert" className="rounded-2xl bg-sale/10 px-4 py-3 text-sm text-sale">{message}</p>}

      <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
        {existing ? (
          <button type="button" onClick={remove} disabled={busy} className="inline-flex items-center gap-1.5 text-sm font-semibold text-sale hover:underline disabled:opacity-50">
            <Trash2 size={15} /> Delete review
          </button>
        ) : <span />}
        <button type="submit" disabled={busy} className="rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone transition hover:bg-forest-deep disabled:opacity-60">
          {busy ? "Saving…" : existing ? "Save changes" : "Post review"}
        </button>
      </div>
    </form>
  );
}

function Thumb({ src, onRemove }: { src: string; onRemove: () => void }) {
  return (
    <span className="relative h-20 w-20">
      <img src={src} alt="" className="h-full w-full rounded-2xl object-cover" />
      <button type="button" onClick={onRemove} aria-label="Remove photo" className="absolute -right-1.5 -top-1.5 grid h-6 w-6 place-items-center rounded-full bg-forest-deep text-bone shadow">
        <X size={13} />
      </button>
    </span>
  );
}
