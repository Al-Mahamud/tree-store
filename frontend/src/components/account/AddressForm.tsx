"use client";

import { useState } from "react";
import { ApiError, request } from "@/lib/client";
import type { SavedAddress, Zone } from "@/lib/types";
import { Field } from "@/components/ui/Field";

const LABELS = ["Home", "Office", "Village home"];

export type AddressValues = { label: string; name: string; phone: string; address: string; area: string; city: string; zone: Zone; note: string; makeDefault: boolean };

export function emptyAddress(name = "", phone = ""): AddressValues {
  return { label: "Home", name, phone, address: "", area: "", city: "Dhaka", zone: "DHAKA", note: "", makeDefault: false };
}

export function addressValues(a: SavedAddress): AddressValues {
  return { label: a.label, name: a.name, phone: a.phone, address: a.address, area: a.area, city: a.city, zone: a.zone, note: a.note ?? "", makeDefault: a.isDefault };
}

/** Add or edit a saved address. Inside or outside Dhaka decides the fee and whether large trees can come. */
export function AddressForm({ initial, id, onSaved, onCancel }: { initial: AddressValues; id?: number; onSaved: () => void; onCancel: () => void }) {
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof AddressValues>(key: K, value: AddressValues[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: "" }));
  };
  const custom = !LABELS.includes(form.label);

  return (
    <form
      noValidate
      className="space-y-4"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setFormError(null);
        try {
          await request(id ? "PUT" : "POST", id ? `/account/addresses/${id}` : "/account/addresses", form);
          onSaved();
        } catch (err) {
          setBusy(false);
          if (err instanceof ApiError) {
            setErrors(err.fields);
            setFormError(err.message);
          } else setFormError("Something went wrong. Please try again.");
        }
      }}
    >
      <fieldset>
        <legend className="mb-1.5 text-xs font-semibold text-ink-soft">Label</legend>
        <div className="flex flex-wrap gap-1.5">
          {[...LABELS, "Other"].map((l) => {
            const chosen = l === "Other" ? custom : form.label === l;
            return <button key={l} type="button" aria-pressed={chosen} onClick={() => set("label", l === "Other" ? (custom ? form.label : "") : l)} className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${chosen ? "border-forest bg-forest text-bone" : "border-line hover:border-forest"}`}>{l}</button>;
          })}
        </div>
        {custom && <Field label="Name this address" error={errors.label} className="mt-3"><input value={form.label} maxLength={20} onChange={(e) => set("label", e.target.value)} /></Field>}
      </fieldset>
      <fieldset>
        <legend className="mb-1.5 text-xs font-semibold text-ink-soft">Where</legend>
        <div className="grid grid-cols-2 gap-2">
          {([["DHAKA", "Inside Dhaka"], ["OUTSIDE", "Outside Dhaka"]] as const).map(([value, label]) => (
            <button key={value} type="button" aria-pressed={form.zone === value} onClick={() => { set("zone", value); if (value === "DHAKA" && !form.city) set("city", "Dhaka"); }} className={`rounded-2xl border px-4 py-3 text-sm font-semibold transition ${form.zone === value ? "border-forest bg-white" : "border-line"}`}>{label}</button>
          ))}
        </div>
        {errors.zone && <span className="mt-1.5 block text-xs text-sale">{errors.zone}</span>}
      </fieldset>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Receiver's name" error={errors.name}><input autoComplete="name" value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
        <Field label="Mobile number" error={errors.phone}><input type="tel" inputMode="tel" autoComplete="tel" value={form.phone} onChange={(e) => set("phone", e.target.value)} /></Field>
        <Field label="Address" hint="House, road, block or village" error={errors.address} className="sm:col-span-2"><input autoComplete="street-address" value={form.address} onChange={(e) => set("address", e.target.value)} /></Field>
        <Field label="Area / thana" error={errors.area}><input value={form.area} onChange={(e) => set("area", e.target.value)} /></Field>
        <Field label="City / district" error={errors.city}><input value={form.city} onChange={(e) => set("city", e.target.value)} /></Field>
        <Field label="Note for the delivery team (optional)" error={errors.note} className="sm:col-span-2"><input value={form.note} onChange={(e) => set("note", e.target.value)} placeholder="e.g. Rooftop garden, call from the gate" /></Field>
      </div>
      <label className="flex items-center gap-2.5 text-sm font-semibold"><input type="checkbox" checked={form.makeDefault} onChange={(e) => set("makeDefault", e.target.checked)} className="h-4 w-4 accent-forest" /> Use this address by default</label>
      {formError && <p role="alert" className="rounded-2xl bg-sale/10 p-3 text-sm text-sale">{formError}</p>}
      <div className="flex gap-2 pt-1">
        <button type="submit" disabled={busy} className="flex-1 rounded-full bg-forest py-3.5 text-sm font-semibold text-bone disabled:opacity-60">{busy ? "Saving…" : "Save address"}</button>
        <button type="button" onClick={onCancel} className="rounded-full border border-line px-6 py-3.5 text-sm font-semibold">Cancel</button>
      </div>
    </form>
  );
}
