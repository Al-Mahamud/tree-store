"use client";

import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Coupon } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { Modal } from "@/components/ui/Modal";
import { Empty, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

type Form = { code: string; description: string; kind: Coupon["kind"]; value: string; maxDiscount: string; minSubtotal: string; startsAt: string; endsAt: string; usageLimit: string; active: boolean };

const toLocal = (iso: string | null) => (iso ? new Date(new Date(iso).getTime() + 6 * 3600_000).toISOString().slice(0, 16) : "");
const fromLocal = (v: string) => (v ? new Date(`${v}:00+06:00`).toISOString() : null);

function toForm(c: Coupon | null): Form {
  return {
    code: c?.code ?? "", description: c?.description ?? "", kind: c?.kind ?? "PERCENT", value: c ? String(c.value) : "",
    maxDiscount: c?.maxDiscount ? String(c.maxDiscount) : "", minSubtotal: c ? String(c.minSubtotal) : "0",
    startsAt: toLocal(c?.startsAt ?? null), endsAt: toLocal(c?.endsAt ?? null), usageLimit: c?.usageLimit ? String(c.usageLimit) : "", active: c?.active ?? true,
  };
}

function what(c: Coupon) {
  if (c.kind === "FREE_SHIPPING") return "Free delivery";
  if (c.kind === "FIXED") return `${taka(c.value)} off`;
  return `${c.value}% off${c.maxDiscount ? ` (up to ${taka(c.maxDiscount)})` : ""}`;
}

export default function CouponsPage() {
  const { notify } = useAdmin();
  const [list, setList] = useState<Coupon[] | null>(null);
  const [editing, setEditing] = useState<Coupon | "new" | null>(null);
  const [form, setForm] = useState<Form>(toForm(null));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const load = () => request<Coupon[]>("GET", "/admin/coupons").then(setList);
  useEffect(() => { void load(); }, []);

  const open = (c: Coupon | "new") => {
    setEditing(c);
    setForm(toForm(c === "new" ? null : c));
    setErrors({});
  };
  const save = async () => {
    const n = (v: string) => (v.trim() === "" ? null : Number(v));
    const body = { ...form, value: n(form.value), maxDiscount: n(form.maxDiscount), minSubtotal: n(form.minSubtotal), usageLimit: n(form.usageLimit), startsAt: fromLocal(form.startsAt), endsAt: fromLocal(form.endsAt) };
    try {
      if (editing === "new") await request("POST", "/admin/coupons", body);
      else if (editing) await request("PUT", `/admin/coupons/${editing.id}`, body);
      setEditing(null);
      notify("Coupon saved");
      void load();
    } catch (e) {
      if (e instanceof ApiError) setErrors(e.fields);
    }
  };
  const f = (label: string, key: keyof Form, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">{label}</span>
      <input value={form[key] as string} onChange={(e) => { setForm({ ...form, [key]: e.target.value }); setErrors({ ...errors, [key]: "" }); }} className={`${input} ${errors[key] ? "border-sale" : ""}`} {...props} />
      {errors[key] && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors[key]}</span>}</label>
  );

  return (
    <>
      <PageHeader title="Coupons" note="Codes shoppers type in the bag."><button type="button" onClick={() => open("new")} className={btn.primary}><Plus size={16} /> New coupon</button></PageHeader>
      {!list ? <Loading /> : list.length === 0 ? <Empty>No coupons yet.</Empty> : (
        <ul className="grid gap-2 md:grid-cols-2 2xl:grid-cols-3">
          {list.map((c) => {
            const expired = c.endsAt && new Date(c.endsAt) < new Date();
            const state = !c.active ? "Off" : expired ? "Expired" : c.usageLimit && c.usedCount >= c.usageLimit ? "Used up" : "Live";
            return (
              <li key={c.id}>
                <button type="button" onClick={() => open(c)} className="w-full rounded-3xl border border-line bg-white p-4 text-left transition hover:border-forest/50">
                  <div className="flex items-center gap-2">
                    <span className="rounded-xl border-2 border-dashed border-forest px-2 py-0.5 font-mono text-sm font-semibold">{c.code}</span>
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${state === "Live" ? "bg-ok/15 text-ok" : "bg-mist text-muted"}`}>{state}</span>
                  </div>
                  <p className="mt-2 font-semibold">{what(c)}{c.minSubtotal > 0 ? <span className="font-normal text-muted"> over {taka(c.minSubtotal)}</span> : null}</p>
                  {c.description && <p className="text-sm text-ink-soft">{c.description}</p>}
                  <p className="mt-1 text-xs text-muted">Used {c.usedCount}{c.usageLimit ? ` of ${c.usageLimit}` : ""} times{c.endsAt ? ` · ends ${new Date(c.endsAt).toLocaleDateString("en-GB", { day: "numeric", month: "short" })}` : ""}</p>
                </button>
              </li>
            );
          })}
        </ul>
      )}
      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "New coupon" : "Edit coupon"}>
        <div className="grid gap-3 sm:grid-cols-2">
          {f("Code", "code", { placeholder: "MONSOON300", className: `${input} font-mono uppercase` })}
          <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Gives</span>
            <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as Coupon["kind"] })} className={input}>
              <option value="PERCENT">A percentage off</option><option value="FIXED">An amount off</option><option value="FREE_SHIPPING">Free delivery</option>
            </select></label>
          {form.kind !== "FREE_SHIPPING" && f(form.kind === "PERCENT" ? "Percent off" : "Taka off", "value", { inputMode: "numeric" })}
          {form.kind === "PERCENT" && f("At most ৳ (optional)", "maxDiscount", { inputMode: "numeric" })}
          {f("Minimum order ৳", "minSubtotal", { inputMode: "numeric" })}
          {f("Uses allowed (empty = no limit)", "usageLimit", { inputMode: "numeric" })}
          {f("Starts (optional)", "startsAt", { type: "datetime-local" })}
          {f("Ends (optional)", "endsAt", { type: "datetime-local" })}
          <div className="sm:col-span-2">{f("Shown to shoppers", "description", { placeholder: "e.g. ৳300 off for the monsoon" })}</div>
          <label className="flex items-center gap-2 text-sm font-semibold sm:col-span-2"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} className="h-4 w-4 accent-forest" /> Active</label>
        </div>
        <div className="mt-5 flex gap-2"><button type="button" onClick={save} className={btn.primary}>Save coupon</button><button type="button" onClick={() => setEditing(null)} className={btn.ghost}>Cancel</button></div>
      </Modal>
    </>
  );
}
