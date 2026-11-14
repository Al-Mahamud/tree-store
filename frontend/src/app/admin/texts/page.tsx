"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { RotateCw, Search } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { TextPage } from "@/lib/admin-types";
import { dateTime } from "@/lib/format";
import { Empty, Loading, PageHeader, Stat, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const kinds = { CODE: "Sign-up code", ORDER_PLACED: "Order confirmed", ORDER_SHIPPED: "On the way", ORDER_CANCELLED: "Cancelled", CARE_REMINDER: "Care reminder" } as const;
const tones = { QUEUED: "bg-lime/40 text-forest-deep", SENDING: "bg-lime/40 text-forest-deep", SENT: "bg-ok/15 text-ok", FAILED: "bg-sale/10 text-sale" } as const;

export default function TextsPage() {
  const { notify } = useAdmin();
  const [status, setStatus] = useState("");
  const [phone, setPhone] = useState("");
  const [search, setSearch] = useState("");
  const [data, setData] = useState<TextPage | null>(null);

  const load = useCallback(() => {
    const q = new URLSearchParams();
    if (status) q.set("status", status);
    if (phone) q.set("phone", phone);
    request<TextPage>("GET", `/admin/texts?${q}`).then(setData);
  }, [status, phone]);
  useEffect(() => {
    load();
    const timer = setInterval(load, 15_000);
    return () => clearInterval(timer);
  }, [load]);

  return (
    <>
      <PageHeader title="Text messages" note="What customers were texted. Codes are hidden once sent." />
      {data && (
        <div className="mb-4 grid grid-cols-3 gap-3">
          <Stat label="Sent today" value={data.counts.SENT ?? 0} note="Last 24 hours" />
          <Stat label="Waiting" value={(data.counts.QUEUED ?? 0) + (data.counts.SENDING ?? 0)} />
          <Stat label="Failed" value={data.counts.FAILED ?? 0} tone={(data.counts.FAILED ?? 0) > 0 ? "warn" : "plain"} />
        </div>
      )}
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); setPhone(search.trim()); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Phone</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Mobile number" className={`${input} pl-9`} /></label>
        <select aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value)} className={`${input} w-auto!`}>
          <option value="">All</option><option value="SENT">Sent</option><option value="QUEUED">Waiting</option><option value="FAILED">Failed</option>
        </select>
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No texts yet.</Empty> : (
        <ul className="divide-y divide-line overflow-hidden rounded-3xl border border-line bg-white">
          {data.items.map((t) => (
            <li key={t.id} className="grid gap-1 px-4 py-3 md:grid-cols-[11rem_1fr_auto] md:items-start md:gap-4">
              <div className="text-sm">
                <p className="font-semibold">{t.phone}</p>
                <p className="text-xs text-muted">{dateTime(t.createdAt)}</p>
              </div>
              <div className="min-w-0 text-sm">
                <p className="text-xs font-semibold text-ink-soft">{kinds[t.kind]}{t.orderNumber && <> · <Link href={`/admin/orders/${t.orderNumber}`} className="text-moss">{t.orderNumber}</Link></>}</p>
                <p className="break-words">{t.body}</p>
                {t.error && <p className="mt-1 text-xs font-semibold text-sale">{t.error}</p>}
              </div>
              <div className="flex items-center gap-2">
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${tones[t.status]}`}>{t.status === "SENT" ? `Sent${t.provider === "log" ? " (log)" : ""}` : t.status.toLowerCase()}</span>
                {t.status === "FAILED" && (
                  <button type="button" className={`${btn.ghost} px-2.5 py-1`} onClick={async () => {
                    try {
                      setData(await request<TextPage>("POST", `/admin/texts/${t.id}/retry`));
                      notify("It will be sent again shortly");
                    } catch (e) {
                      notify(e instanceof ApiError ? e.message : "Couldn't retry.");
                    }
                  }}><RotateCw size={14} /> Retry</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
