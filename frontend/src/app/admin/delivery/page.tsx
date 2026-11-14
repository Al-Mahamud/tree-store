"use client";

import { useEffect, useState } from "react";
import { Truck } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { DeliveryFees } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { dateTime } from "@/lib/format";
import { Card, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

type Form = { insideDhaka: string; outsideDhaka: string; freeOver: string };

export default function DeliveryPage() {
  const { notify } = useAdmin();
  const [fees, setFees] = useState<DeliveryFees | null>(null);
  const [form, setForm] = useState<Form | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const show = (f: DeliveryFees) => {
    setFees(f);
    setForm({ insideDhaka: String(f.insideDhaka), outsideDhaka: String(f.outsideDhaka), freeOver: String(f.freeOver) });
  };
  useEffect(() => {
    request<DeliveryFees>("GET", "/admin/delivery").then(show);
  }, []);
  if (!fees || !form) return <Loading />;

  const field = (key: keyof Form, label: string, hint: string) => (
    <label className="block">
      <span className="mb-1 block text-xs font-semibold text-ink-soft">{label}</span>
      <span className="relative block"><span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted">৳</span>
        <input inputMode="numeric" value={form[key]} onChange={(e) => { setForm({ ...form, [key]: e.target.value.replace(/[^0-9]/g, "") }); setErrors({ ...errors, [key]: "" }); }} className={`${input} pl-7 tabular-nums ${errors[key] ? "border-sale" : ""}`} /></span>
      {errors[key] ? <span className="mt-1 block text-xs font-semibold text-sale">{errors[key]}</span> : <span className="mt-1 block text-xs text-muted">{hint}</span>}
    </label>
  );
  const changed = form.insideDhaka !== String(fees.insideDhaka) || form.outsideDhaka !== String(fees.outsideDhaka) || form.freeOver !== String(fees.freeOver);

  return (
    <>
      <PageHeader title="Delivery" note="What shoppers pay to have plants brought to them." />
      <div className="grid gap-4 lg:grid-cols-[1fr_20rem]">
        <Card title="Delivery fees">
          <form
            className="space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              try {
                show(await request<DeliveryFees>("PUT", "/admin/delivery", { insideDhaka: Number(form.insideDhaka), outsideDhaka: Number(form.outsideDhaka), freeOver: Number(form.freeOver) }));
                notify("Delivery fees saved. The shop shows them within a minute.");
              } catch (err) {
                if (err instanceof ApiError) setErrors(err.fields);
              } finally {
                setBusy(false);
              }
            }}
          >
            <div className="grid gap-4 sm:grid-cols-3">
              {field("insideDhaka", "Inside Dhaka", "Our own van, 1–2 days")}
              {field("outsideDhaka", "Outside Dhaka", "By courier, 2–4 days")}
              {field("freeOver", "Free delivery from", "Order total after coupons")}
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <button type="submit" disabled={busy || !changed} className={btn.primary}>Save fees</button>
              {fees.updatedAt ? <span className="text-xs text-muted">Last changed {dateTime(fees.updatedAt)}{fees.updatedBy ? ` by ${fees.updatedBy.replace(/^staff:/, "")}` : ""}</span> : <span className="text-xs text-muted">Using the shop&rsquo;s starting fees.</span>}
            </div>
          </form>
        </Card>
        <Card title="How it looks">
          <ul className="space-y-2 text-sm">
            <li className="flex items-center justify-between rounded-2xl bg-bone px-3 py-2.5"><span>Inside Dhaka</span><strong>{taka(Number(form.insideDhaka) || 0)}</strong></li>
            <li className="flex items-center justify-between rounded-2xl bg-bone px-3 py-2.5"><span>Outside Dhaka</span><strong>{taka(Number(form.outsideDhaka) || 0)}</strong></li>
            <li className="flex items-center justify-between rounded-2xl bg-lime/40 px-3 py-2.5"><span>Orders from {taka(Number(form.freeOver) || 0)}</span><strong>Free</strong></li>
          </ul>
          <p className="mt-3 flex gap-2 text-xs text-muted"><Truck size={14} className="mt-0.5 shrink-0 text-clay" /> Large trees marked &ldquo;Inside Dhaka only&rdquo; in the plant editor can&rsquo;t be ordered to addresses outside Dhaka.</p>
        </Card>
      </div>
    </>
  );
}
