"use client";

import { useEffect, useState } from "react";
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { CategoryAdmin } from "@/lib/admin-types";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { PictureButton } from "@/components/admin/PictureButton";
import { useAdmin } from "@/components/admin/AdminShell";

type Editing = { id: number | null; parentId: number | null; name: string; description: string };
const iconBtn = "rounded-full p-2 text-ink-soft hover:bg-white";

export default function CategoriesPage() {
  const { notify } = useAdmin();
  const [tree, setTree] = useState<CategoryAdmin[] | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    request<CategoryAdmin[]>("GET", "/admin/categories").then(setTree);
  }, []);

  const call = async (method: string, path: string, body: unknown, done: string) => {
    try {
      setTree(await request<CategoryAdmin[]>(method, path, body));
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
  const edit = (c: CategoryAdmin | null, parentId: number | null) => {
    setErrors({});
    setEditing(c ? { id: c.id, parentId: c.parentId, name: c.name, description: c.description ?? "" } : { id: null, parentId, name: "", description: "" });
  };

  const row = (c: CategoryAdmin, top: boolean) => (
    <div className={`flex items-center gap-3 rounded-2xl px-2.5 py-2 ${top ? "bg-bone" : ""}`}>
      <PictureButton<CategoryAdmin[]> image={c.image} path={`/admin/categories/${c.id}/image`} label={`Picture for ${c.name}`} onDone={setTree} className={top ? "h-14 w-14" : "h-11 w-11"} />
      <span className="min-w-0 flex-1">
        <span className={`block truncate ${top ? "font-display text-xl leading-tight" : "font-semibold"}`}>{c.name}</span>
        <span className="block truncate text-xs text-muted">/c/{c.slug} · {c.products} plant{c.products === 1 ? "" : "s"}{c.description ? ` · ${c.description}` : ""}</span>
      </span>
      <span className="flex shrink-0 gap-0.5">
        <button type="button" aria-label={`Move ${c.name} up`} onClick={() => call("POST", `/admin/categories/${c.id}/move`, { direction: -1 }, "Moved")} className={iconBtn}><ArrowUp size={15} /></button>
        <button type="button" aria-label={`Move ${c.name} down`} onClick={() => call("POST", `/admin/categories/${c.id}/move`, { direction: 1 }, "Moved")} className={iconBtn}><ArrowDown size={15} /></button>
        <button type="button" aria-label={`Edit ${c.name}`} onClick={() => edit(c, c.parentId)} className={iconBtn}><Pencil size={15} /></button>
        <button type="button" aria-label={`Delete ${c.name}`} onClick={() => window.confirm(`Delete “${c.name}”?`) && call("DELETE", `/admin/categories/${c.id}`, undefined, `${c.name} deleted`)} className="rounded-full p-2 text-sale hover:bg-white"><Trash2 size={15} /></button>
      </span>
    </div>
  );

  return (
    <>
      <PageHeader title="Categories" note="Groups and the categories inside them, in the order shoppers see them. Click a picture to change it.">
        <button type="button" onClick={() => edit(null, null)} className={btn.primary}><Plus size={16} /> New group</button>
      </PageHeader>
      {!tree ? <Loading /> : (
        <div className="grid gap-3 xl:grid-cols-2">
          {tree.map((top) => (
            <Card key={top.id}>
              {row(top, true)}
              <div className="mt-1 space-y-0.5 pl-3">
                {top.children.map((c) => <div key={c.id}>{row(c, false)}</div>)}
                <button type="button" onClick={() => edit(null, top.id)} className="mt-1 inline-flex items-center gap-1.5 rounded-full px-3 py-2 text-xs font-semibold text-moss hover:bg-bone"><Plus size={14} /> Add a category to {top.name}</button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <p className="mt-3 text-xs text-muted">A category&rsquo;s web address is fixed when it&rsquo;s made, so renaming never breaks links. Only empty categories can be deleted; archived plants count.</p>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing?.id ? "Edit category" : editing?.parentId ? "New category" : "New group"}>
        {editing && (
          <div className="space-y-4">
            <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Name</span>
              <input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} className={`${input} ${errors.name ? "border-sale" : ""}`} />
              {errors.name && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors.name}</span>}</label>
            <label className="block"><span className="mb-1 block text-xs font-semibold text-ink-soft">Short description (shown on the category page)</span>
              <textarea rows={2} value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} className={`${input} ${errors.description ? "border-sale" : ""}`} />
              {errors.description && <span className="mt-0.5 block text-xs font-semibold text-sale">{errors.description}</span>}</label>
            {errors.parentId && <p className="text-sm font-semibold text-sale">{errors.parentId}</p>}
            <button type="button" className={btn.primary} onClick={async () => {
              const body = { name: editing.name, description: editing.description, parentId: editing.parentId };
              const ok = editing.id ? await call("PUT", `/admin/categories/${editing.id}`, body, "Saved") : await call("POST", "/admin/categories", body, `${editing.name} added`);
              if (ok) setEditing(null);
            }}>Save</button>
          </div>
        )}
      </Modal>
    </>
  );
}
