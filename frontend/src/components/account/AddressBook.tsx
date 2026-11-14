"use client";

import { useCallback, useEffect, useState } from "react";
import { MapPin, Pencil, Plus, Star, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { SavedAddress } from "@/lib/types";
import { Modal } from "@/components/ui/Modal";
import { useCart } from "@/components/cart/CartProvider";
import { useAccount } from "./AccountProvider";
import { AddressForm, addressValues, emptyAddress } from "./AddressForm";

export function AddressBook() {
  const { me } = useAccount();
  const { notify } = useCart();
  const [list, setList] = useState<SavedAddress[] | null>(null);
  const [editing, setEditing] = useState<SavedAddress | "new" | null>(null);

  const load = useCallback(() => request<SavedAddress[]>("GET", "/account/addresses").then(setList).catch(() => setList([])), []);
  useEffect(() => {
    void load();
  }, [load]);

  const act = async (method: string, path: string, done: string) => {
    try {
      await request(method, path);
      await load();
      notify(done);
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Something went wrong. Please try again.");
    }
  };

  if (!list) return <div className="h-64 animate-pulse rounded-[2rem] bg-white/60" aria-busy="true" />;

  return (
    <section aria-labelledby="addresses-title">
      <div className="mb-4 flex items-end justify-between gap-3">
        <div>
          <h2 id="addresses-title" className="font-display text-3xl">Saved addresses</h2>
          <p className="text-sm text-muted">Pick one at checkout instead of typing it again.</p>
        </div>
        {list.length > 0 && list.length < 10 && (
          <button type="button" onClick={() => setEditing("new")} className="inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full bg-forest px-5 py-2.5 text-sm font-semibold text-bone"><Plus size={16} /> Add address</button>
        )}
      </div>
      {list.length === 0 ? (
        <button type="button" onClick={() => setEditing("new")} className="grid w-full place-items-center rounded-[2rem] border-2 border-dashed border-line bg-white/60 px-6 py-14 text-center hover:border-forest/50">
          <span className="grid h-14 w-14 place-items-center rounded-full bg-sage text-forest"><MapPin size={26} /></span>
          <span className="mt-3 font-display text-2xl">Add your first address</span>
          <span className="mt-1 text-sm text-muted">Home, office, or family anywhere in Bangladesh.</span>
        </button>
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {list.map((a) => (
            <li key={a.id} className={`flex flex-col rounded-[2rem] border-2 bg-white/70 p-5 ${a.isDefault ? "border-forest" : "border-transparent"}`}>
              <div className="flex items-center gap-2">
                <span className="font-display text-2xl">{a.label}</span>
                {a.isDefault && <span className="rounded-full bg-lime/60 px-2.5 py-0.5 text-xs font-semibold text-forest-deep">Default</span>}
                <span className="ml-auto rounded-full bg-mist px-2.5 py-0.5 text-xs font-semibold">{a.zone === "DHAKA" ? "Inside Dhaka" : "Outside Dhaka"}</span>
              </div>
              <p className="mt-2 text-sm font-semibold">{a.name} · {a.phone}</p>
              <p className="text-sm text-ink-soft">{a.address}, {a.area}, {a.city}</p>
              {a.note && <p className="mt-1 text-xs italic text-muted">“{a.note}”</p>}
              <div className="mt-4 flex flex-wrap gap-1.5 pt-1">
                <button type="button" onClick={() => setEditing(a)} className="inline-flex items-center gap-1.5 rounded-full bg-mist px-3.5 py-2 text-xs font-semibold hover:bg-sage"><Pencil size={13} /> Edit</button>
                {!a.isDefault && (
                  <button type="button" onClick={() => act("POST", `/account/addresses/${a.id}/default`, `${a.label} is now your default address.`)} className="inline-flex items-center gap-1.5 rounded-full bg-mist px-3.5 py-2 text-xs font-semibold hover:bg-sage"><Star size={13} /> Make default</button>
                )}
                <button
                  type="button"
                  onClick={() => window.confirm(`Remove “${a.label}”?`) && act("DELETE", `/account/addresses/${a.id}`, "Address removed.")}
                  className="ml-auto inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-semibold text-sale hover:bg-sale/10"
                >
                  <Trash2 size={13} /> Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "New address" : "Edit address"}>
        {editing !== null && (
          <AddressForm
            key={editing === "new" ? "new" : editing.id}
            id={editing === "new" ? undefined : editing.id}
            initial={editing === "new" ? emptyAddress(me?.name ?? "", me?.phone ?? "") : addressValues(editing)}
            onCancel={() => setEditing(null)}
            onSaved={async () => {
              setEditing(null);
              await load();
              notify("Address saved.");
            }}
          />
        )}
      </Modal>
    </section>
  );
}
