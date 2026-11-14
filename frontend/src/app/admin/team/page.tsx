"use client";

import { useEffect, useState } from "react";
import { KeyRound, Plus, UserRound } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { StaffRow } from "@/lib/admin-types";
import { dateTime } from "@/lib/format";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

type Dialog = { kind: "add" } | { kind: "reset"; person: StaffRow } | { kind: "mine" } | null;

export default function TeamPage() {
  const { staff: me, notify } = useAdmin();
  const [list, setList] = useState<StaffRow[] | null>(null);
  const [dialog, setDialog] = useState<Dialog>(null);
  const [form, setForm] = useState({ name: "", phone: "", password: "", currentPassword: "", newPassword: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    request<StaffRow[]>("GET", "/admin/staff").then(setList);
  }, []);

  const open = (d: Dialog) => {
    setDialog(d);
    setForm({ name: "", phone: "", password: "", currentPassword: "", newPassword: "" });
    setErrors({});
  };
  const run = async (work: () => Promise<void>, done: string) => {
    setBusy(true);
    try {
      await work();
      notify(done);
      setDialog(null);
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields);
        if (!Object.keys(e.fields).length) notify(e.message);
      } else notify("Something went wrong.");
    } finally {
      setBusy(false);
    }
  };
  const field = (label: string, key: keyof typeof form, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">{label}</span>
      <input value={form[key]} onChange={(e) => { setForm({ ...form, [key]: e.target.value }); setErrors({ ...errors, [key]: "" }); }} className={`${input} ${errors[key] ? "border-sale" : ""}`} {...props} />
      {errors[key] && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors[key]}</span>}</label>
  );

  return (
    <>
      <PageHeader title="Team" note="Everyone who can use this admin area.">
        <button type="button" onClick={() => open({ kind: "mine" })} className={btn.ghost}><KeyRound size={15} /> My password</button>
        <button type="button" onClick={() => open({ kind: "add" })} className={btn.primary}><Plus size={16} /> Add someone</button>
      </PageHeader>
      {!list ? <Loading /> : (
        <Card>
          <ul className="divide-y divide-line">
            {list.map((p) => (
              <li key={p.id} className={`flex flex-wrap items-center gap-3 py-3 ${p.active ? "" : "opacity-60"}`}>
                <span className={`grid h-10 w-10 place-items-center rounded-full font-display text-lg ${p.active ? "bg-forest text-lime" : "bg-mist text-muted"}`}>{p.name.charAt(0)}</span>
                <span className="min-w-0 flex-1">
                  <span className="block font-semibold">{p.name}{p.id === me.id && <span className="ml-1.5 rounded-md bg-lime/50 px-1.5 py-0.5 text-xs font-semibold">You</span>}</span>
                  <span className="text-xs text-muted">{p.phone} · {p.lastSignedIn ? `last signed in ${dateTime(p.lastSignedIn)}` : "never signed in"}</span>
                </span>
                {p.id !== me.id && (
                  <span className="flex gap-1.5">
                    <button type="button" onClick={() => open({ kind: "reset", person: p })} className={`${btn.ghost} px-3 py-1.5 text-xs`}>New password</button>
                    <button
                      type="button"
                      onClick={() => {
                        if (p.active && !window.confirm(`Switch off ${p.name}? They are signed out at once.`)) return;
                        void run(async () => setList(await request<StaffRow[]>("POST", `/admin/staff/${p.id}/active`, { active: !p.active })), p.active ? `${p.name} is switched off` : `${p.name} can sign in again`);
                      }}
                      className={`${p.active ? btn.danger : btn.primary} px-3 py-1.5 text-xs`}
                    >
                      {p.active ? "Switch off" : "Switch on"}
                    </button>
                  </span>
                )}
              </li>
            ))}
          </ul>
        </Card>
      )}
      <p className="mt-3 flex items-center gap-2 text-xs text-muted"><UserRound size={14} /> Everyone on the team can do everything in the admin area. Switch people off when they leave.</p>

      <Modal open={dialog !== null} onClose={() => setDialog(null)} title={dialog?.kind === "add" ? "Add someone" : dialog?.kind === "reset" ? `New password for ${dialog.person.name}` : "My password"}>
        {dialog?.kind === "add" && (
          <div className="space-y-3">
            {field("Name", "name", { placeholder: "e.g. Jamal (gardener)" })}
            {field("Mobile number", "phone", { type: "tel", placeholder: "01XXXXXXXXX" })}
            {field("Starting password", "password", { type: "text", autoComplete: "off" })}
            <p className="text-xs text-muted">At least 10 characters. Give it to them in person; they should change it under My password.</p>
            <button type="button" disabled={busy} onClick={() => run(async () => setList(await request<StaffRow[]>("POST", "/admin/staff", { name: form.name, phone: form.phone, password: form.password })), `${form.name} added`)} className={btn.primary}>Add to the team</button>
          </div>
        )}
        {dialog?.kind === "reset" && (
          <div className="space-y-3">
            {field("New password", "password", { type: "text", autoComplete: "off" })}
            <p className="text-xs text-muted">They are signed out everywhere and sign in again with this password.</p>
            <button type="button" disabled={busy} onClick={() => run(async () => setList(await request<StaffRow[]>("POST", `/admin/staff/${dialog.person.id}/password`, { password: form.password })), "Password changed")} className={btn.primary}>Set password</button>
          </div>
        )}
        {dialog?.kind === "mine" && (
          <div className="space-y-3">
            {field("Current password", "currentPassword", { type: "password", autoComplete: "current-password" })}
            {field("New password", "newPassword", { type: "password", autoComplete: "new-password" })}
            <p className="text-xs text-muted">At least 10 characters. Your other devices are signed out.</p>
            <button type="button" disabled={busy} onClick={() => run(async () => { await request("POST", "/admin/me/password", { currentPassword: form.currentPassword, newPassword: form.newPassword }); }, "Your password is changed")} className={btn.primary}>Change my password</button>
          </div>
        )}
      </Modal>
    </>
  );
}
