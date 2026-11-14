"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowDown, ArrowUp, Pencil, Plus } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { CollectionAdmin } from "@/lib/admin-types";
import { Modal } from "@/components/ui/Modal";
import { Empty, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { PictureButton } from "@/components/admin/PictureButton";
import { useAdmin } from "@/components/admin/AdminShell";

type Editing = { id: number | null; name: string; tagline: string; description: string; active: boolean };
const iconBtn = "rounded-full p-2 text-ink-soft hover:bg-bone";

export default function CollectionsPage() {
  const { notify } = useAdmin();
  const [list, setList] = useState<CollectionAdmin[] | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    request<CollectionAdmin[]>("GET", "/admin/collections").then(setList);
  }, []);

  const call = async (method: string, path: string, body: unknown, done: string) => {
    try {
      setList(await request<CollectionAdmin[]>(method, path, body));
      notify(done);
      return true;
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields);
        if (!Object.keys(e.fields).length) notify(e.message);
      }
      return false;
    }
  };

  return (
    <>
      <PageHeader title="Collections" note="Themed groups on the home page, like Monsoon Planting. Add plants to them in the plant editor.">
        <button type="button" onClick={() => { setErrors({}); setEditing({ id: null, name: "", tagline: "", description: "", active: true }); }} className={btn.primary}><Plus size={16} /> New collection</button>
      </PageHeader>
      {!list ? <Loading /> : list.length === 0 ? <Empty>No collections yet.</Empty> : (
        <ul className="space-y-2">
          {list.map((c, i) => (
            <li key={c.id} className={`flex flex-wrap items-center gap-4 rounded-3xl border border-line bg-white p-3 ${c.active ? "" : "opacity-70"}`}>
              <PictureButton<CollectionAdmin[]> image={c.image} path={`/admin/collections/${c.id}/image`} label={`Banner for ${c.name}`} onDone={setList} className="h-20 w-36" />
              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-2"><span className="font-display text-2xl leading-tight">{c.name}</span>{!c.active && <span className="rounded-full bg-mist px-2.5 py-0.5 text-xs font-semibold text-muted">Hidden</span>}</span>
                {c.tagline && <span className="block text-sm text-ink-soft">{c.tagline}</span>}
                <span className="text-xs text-muted">{c.active ? <Link href={`/collections/${c.slug}`} target="_blank" className="hover:text-moss">/collections/{c.slug}</Link> : `/collections/${c.slug}`} · {c.products} plant{c.products === 1 ? "" : "s"}</span>
              </span>
              <span className="flex gap-0.5">
                <button type="button" aria-label={`Move ${c.name} up`} disabled={i === 0} onClick={() => call("POST", `/admin/collections/${c.id}/move`, { direction: -1 }, "Moved")} className={`${iconBtn} disabled:opacity-30`}><ArrowUp size={15} /></button>
                <button type="button" aria-label={`Move ${c.name} down`} disabled={i === list.length - 1} onClick={() => call("POST", `/admin/collections/${c.id}/move`, { direction: 1 }, "Moved")} className={`${iconBtn} disabled:opacity-30`}><ArrowDown size={15} /></button>
                <button type="button" aria-label={`Edit ${c.name}`} onClick={() => { setErrors({}); setEditing({ id: c.id, name: c.name, tagline: c.tagline ?? "", description: c.description ?? "", active: c.active }); }} className={iconBtn}><Pencil size={15} /></button>
              </span>
            </li>
          ))}
        </ul>
      )}
      <p className="mt-3 text-xs text-muted">Collections are never deleted, so old links keep working. Hide one to take it off the shop.</p>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing?.id ? "Edit collection" : "New collection"}>
        {editing && (
          <div className="space-y-4">
            {(["name", "tagline"] as const).map((key) => (
              <label key={key} className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">{key === "name" ? "Name" : "Tagline (one short line)"}</span>
                <input value={editing[key]} onChange={(e) => setEditing({ ...editing, [key]: e.target.value })} className={`${input} ${errors[key] ? "border-sale" : ""}`} />
                {errors[key] && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors[key]}</span>}</label>
            ))}
            <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Description</span>
              <textarea rows={3} value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} className={`${input} ${errors.description ? "border-sale" : ""}`} />
              {errors.description && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors.description}</span>}</label>
            <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} className="h-4 w-4 accent-forest" /> Show in the shop</label>
            <button type="button" className={btn.primary} onClick={async () => {
              const body = { name: editing.name, tagline: editing.tagline, description: editing.description, active: editing.active };
              const ok = editing.id ? await call("PUT", `/admin/collections/${editing.id}`, body, "Saved") : await call("POST", "/admin/collections", body, `${editing.name} added`);
              if (ok) setEditing(null);
            }}>Save</button>
          </div>
        )}
      </Modal>
    </>
  );
}
