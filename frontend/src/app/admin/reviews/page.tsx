"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Eye, EyeOff, MessageSquareReply, Search } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { AdminReview, AdminReviewPage } from "@/lib/admin-types";
import { dateTime } from "@/lib/format";
import { Empty, FilterTabs, Loading, PageHeader, Pager, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";
import { Stars } from "@/components/ui/Stars";

type Filter = "ALL" | "UNANSWERED" | "LOW" | "PHOTOS" | "HIDDEN";
const tabs: { value: Filter; label: string; count?: string }[] = [
  { value: "ALL", label: "All", count: "ALL" },
  { value: "UNANSWERED", label: "No reply yet", count: "UNANSWERED" },
  { value: "LOW", label: "1–2 stars", count: "LOW" },
  { value: "PHOTOS", label: "With photos", count: "PHOTOS" },
  { value: "HIDDEN", label: "Hidden", count: "HIDDEN" },
];

/** Every review, newest first. Staff hide what breaks the rules and reply where it helps. */
export default function ReviewsPage() {
  const [filter, setFilter] = useState<Filter>("ALL");
  const [search, setSearch] = useState("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminReviewPage | null>(null);

  const load = useCallback(() => {
    const params = new URLSearchParams({ filter, page: String(page) });
    if (q) params.set("q", q);
    return request<AdminReviewPage>("GET", `/admin/reviews?${params}`).then(setData);
  }, [filter, q, page]);
  useEffect(() => {
    void load();
  }, [load]);

  return (
    <>
      <PageHeader title="Reviews" note="Reviews show on plant pages as soon as they're written. Hide ones that are abusive, share personal details or aren't about the plant." />
      <FilterTabs tabs={tabs} value={filter} counts={data?.counts} alert="LOW" onChange={(f) => { setFilter(f); setPage(0); }} />
      <form className="mb-4 flex gap-2" onSubmit={(e) => { e.preventDefault(); setQ(search.trim()); setPage(0); }}>
        <label className="relative min-w-60 flex-1">
          <span className="sr-only">Search reviews</span>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Plant, name, words or mobile number" className={`${input} pl-9`} />
        </label>
        <button type="submit" className={btn.ghost}>Search</button>
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No reviews here.</Empty> : (
        <>
          <ul className="space-y-3">
            {data.items.map((r) => <ReviewRow key={r.id} review={r} onChanged={load} />)}
          </ul>
          <Pager page={data.page} pageSize={data.pageSize} total={data.total} onPage={setPage} />
        </>
      )}
    </>
  );
}

/* eslint-disable @next/next/no-img-element -- plant and review photos are served by the backend as-is. */
function ReviewRow({ review: r, onChanged }: { review: AdminReview; onChanged: () => Promise<void> }) {
  const { notify } = useAdmin();
  const [replying, setReplying] = useState(false);
  const [reply, setReply] = useState(r.reply ?? "");
  const [busy, setBusy] = useState(false);
  const hidden = r.status === "HIDDEN";

  const change = async (body: { status?: string; reply?: string }, done: string) => {
    setBusy(true);
    try {
      await request("PATCH", `/admin/reviews/${r.id}`, body);
      await onChanged();
      notify(done);
      setReplying(false);
    } catch (e) {
      notify(e instanceof ApiError ? (Object.values(e.fields)[0] ?? e.message) : "Couldn't save that.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <li className={`rounded-3xl border bg-white p-4 md:p-5 ${hidden ? "border-dashed border-line opacity-75" : "border-line"}`}>
      <div className="flex gap-4">
        <Link href={`/p/${r.slug}#reviews`} target="_blank" className="h-16 w-13 shrink-0 overflow-hidden rounded-xl bg-mist">
          {r.image && <img src={r.image} alt="" className="h-full w-full object-cover" />}
        </Link>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <Stars value={r.rating} />
            <Link href={`/p/${r.slug}#reviews`} target="_blank" className="text-sm font-semibold hover:underline">{r.product}</Link>
            {hidden && <span className="rounded-full bg-clay/10 px-2 py-0.5 text-xs font-semibold text-clay">Hidden</span>}
          </div>
          {r.title && <p className="mt-2 font-semibold">{r.title}</p>}
          <p className="mt-1 whitespace-pre-line text-sm text-ink-soft">{r.body}</p>
          {r.photos.length > 0 && (
            <div className="mt-3 flex gap-2">
              {r.photos.map((p) => <a key={p.id} href={p.url} target="_blank" rel="noreferrer"><img src={p.url} alt="Customer photo" className="h-16 w-16 rounded-xl object-cover" /></a>)}
            </div>
          )}
          <p className="mt-3 text-xs text-muted">
            {r.author} · {r.phone}{r.bought && ` · ${r.bought}`}
            {r.orderNumber && <> · <Link href={`/admin/orders/${r.orderNumber}`} className="font-semibold text-moss">{r.orderNumber}</Link></>}
            {` · ${dateTime(r.createdAt)}`}
          </p>

          {r.reply && !replying && (
            <div className="mt-3 rounded-2xl bg-mist p-3 text-sm">
              <p className="text-xs font-semibold text-moss">Your reply{r.repliedBy && ` · ${r.repliedBy}`}{r.repliedAt && ` · ${dateTime(r.repliedAt)}`}</p>
              <p className="mt-1 whitespace-pre-line text-ink-soft">{r.reply}</p>
            </div>
          )}
          {replying && (
            <form className="mt-3" onSubmit={(e) => { e.preventDefault(); void change({ reply }, reply.trim() ? "Reply posted" : "Reply removed"); }}>
              <label className="sr-only" htmlFor={`reply-${r.id}`}>Reply</label>
              <textarea id={`reply-${r.id}`} value={reply} onChange={(e) => setReply(e.target.value)} rows={3} maxLength={1000} className={`${input} resize-y`} placeholder="Thank them, or explain how you'll help. Shown under the review." autoFocus />
              <div className="mt-2 flex flex-wrap gap-2">
                <button type="submit" disabled={busy} className={btn.primary}>{reply.trim() ? "Post reply" : "Remove reply"}</button>
                <button type="button" onClick={() => { setReplying(false); setReply(r.reply ?? ""); }} className={btn.ghost}>Cancel</button>
              </div>
            </form>
          )}

          {!replying && (
            <div className="mt-3 flex flex-wrap gap-2">
              <button type="button" onClick={() => setReplying(true)} className={btn.ghost}><MessageSquareReply size={15} /> {r.reply ? "Edit reply" : "Reply"}</button>
              <button type="button" disabled={busy} onClick={() => change({ status: hidden ? "PUBLISHED" : "HIDDEN" }, hidden ? "Review shown again" : "Review hidden")} className={btn.ghost}>
                {hidden ? <><Eye size={15} /> Show again</> : <><EyeOff size={15} /> Hide</>}
              </button>
            </div>
          )}
        </div>
      </div>
    </li>
  );
}
