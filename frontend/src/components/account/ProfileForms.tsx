"use client";

import { useState } from "react";
import { Check } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Profile } from "@/lib/types";
import { Field } from "@/components/ui/Field";
import { useAccount } from "./AccountProvider";

export function ProfileForms() {
  const { me, setMe } = useAccount();
  if (!me) return null;
  return (
    <div className="space-y-4">
      <DetailsForm me={me} onSaved={setMe} />
      <PasswordForm />
    </div>
  );
}

function DetailsForm({ me, onSaved }: { me: Profile; onSaved: (p: Profile) => void }) {
  const [form, setForm] = useState({ name: me.name, email: me.email ?? "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [state, setState] = useState<"idle" | "busy" | "saved">("idle");
  return (
    <form
      noValidate
      className="rounded-[2rem] bg-white/60 p-6 md:p-8"
      onSubmit={async (e) => {
        e.preventDefault();
        setState("busy");
        try {
          onSaved(await request<Profile>("PUT", "/account/profile", form));
          setErrors({});
          setState("saved");
        } catch (err) {
          setState("idle");
          setErrors(err instanceof ApiError ? err.fields : {});
        }
      }}
    >
      <h2 className="font-display text-3xl">Your details</h2>
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <Field label="Full name" error={errors.name}><input autoComplete="name" value={form.name} onChange={(e) => { setForm({ ...form, name: e.target.value }); setState("idle"); }} /></Field>
        <Field label="Email (optional)" error={errors.email}><input type="email" autoComplete="email" value={form.email} onChange={(e) => { setForm({ ...form, email: e.target.value }); setState("idle"); }} /></Field>
        <Field label="Mobile number" hint="You sign in with this number, so it can't be changed here." className="sm:col-span-2">
          <input value={me.phone} readOnly className="text-muted" />
        </Field>
      </div>
      <div className="mt-4 flex items-center gap-3">
        <button type="submit" disabled={state === "busy"} className="rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone disabled:opacity-60">{state === "busy" ? "Saving…" : "Save details"}</button>
        {state === "saved" && <span className="inline-flex items-center gap-1 text-sm font-semibold text-ok"><Check size={16} /> Saved</span>}
      </div>
    </form>
  );
}

function PasswordForm() {
  const [form, setForm] = useState({ currentPassword: "", newPassword: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [state, setState] = useState<"idle" | "busy" | "saved">("idle");
  const [formError, setFormError] = useState<string | null>(null);
  return (
    <form
      noValidate
      className="rounded-[2rem] bg-white/60 p-6 md:p-8"
      onSubmit={async (e) => {
        e.preventDefault();
        setState("busy");
        setFormError(null);
        try {
          await request("POST", "/account/password", form);
          setForm({ currentPassword: "", newPassword: "" });
          setErrors({});
          setState("saved");
        } catch (err) {
          setState("idle");
          if (err instanceof ApiError) {
            setErrors(err.fields);
            if (Object.keys(err.fields).length === 0) setFormError(err.message);
          } else setFormError("Something went wrong. Please try again.");
        }
      }}
    >
      <h2 className="font-display text-3xl">Change password</h2>
      <p className="mt-1 text-sm text-muted">You&rsquo;ll stay signed in here; other phones and computers will be signed out.</p>
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <Field label="Current password" error={errors.currentPassword}><input type="password" autoComplete="current-password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} /></Field>
        <Field label="New password" error={errors.newPassword} hint="At least 8 characters."><input type="password" autoComplete="new-password" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} /></Field>
      </div>
      {formError && <p role="alert" className="mt-3 rounded-2xl bg-sale/10 p-3 text-sm text-sale">{formError}</p>}
      <div className="mt-4 flex items-center gap-3">
        <button type="submit" disabled={state === "busy"} className="rounded-full border border-forest px-7 py-3 text-sm font-semibold disabled:opacity-60">{state === "busy" ? "Changing…" : "Change password"}</button>
        {state === "saved" && <span className="inline-flex items-center gap-1 text-sm font-semibold text-ok"><Check size={16} /> Password changed</span>}
      </div>
    </form>
  );
}
