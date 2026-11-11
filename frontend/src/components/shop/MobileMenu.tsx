"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, X } from "lucide-react";
import type { CategoryNode, Collection } from "@/lib/types";
import { Drawer } from "@/components/ui/Drawer";

export function MobileMenu({ categories, collections }: { categories: CategoryNode[]; collections: Collection[] }) {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  // eslint-disable-next-line react-hooks/set-state-in-effect -- close the menu after navigating.
  useEffect(() => setOpen(false), [pathname]);
  return (
    <>
      <button type="button" aria-label="Open menu" onClick={() => setOpen(true)} className="grid h-10 w-10 place-items-center rounded-full hover:bg-mist lg:hidden">
        <Menu size={22} strokeWidth={1.7} />
      </button>
      <Drawer open={open} onClose={() => setOpen(false)} side="left" label="Menu">
        <div className="flex h-16 items-center justify-between px-5">
          <span className="kicker text-muted">Browse</span>
          <button type="button" aria-label="Close menu" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-full bg-mist">
            <X size={18} />
          </button>
        </div>
        <nav className="flex-1 space-y-6 overflow-y-auto px-5 pb-6">
          {categories.map((top) => (
            <div key={top.id}>
              <Link href={`/c/${top.slug}`} className="font-display text-3xl">{top.name}</Link>
              {top.children.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {top.children.map((child) => (
                    <Link key={child.id} href={`/c/${child.slug}`} className="rounded-full border border-line px-3.5 py-1.5 text-sm">
                      {child.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
          <div>
            <p className="kicker mb-3 text-muted">Collections</p>
            <div className="flex flex-wrap gap-2">
              {collections.map((c) => (
                <Link key={c.id} href={`/collections/${c.slug}`} className="rounded-full bg-mist px-3.5 py-1.5 text-sm">{c.name}</Link>
              ))}
              <Link href="/shop?sale=true" className="rounded-full bg-sale/10 px-3.5 py-1.5 text-sm font-semibold text-sale">Offers</Link>
            </div>
          </div>
        </nav>
        <div className="grid grid-cols-2 gap-3 p-5">
          <Link href="/account" className="rounded-full bg-forest py-3 text-center text-sm font-semibold text-bone">Account</Link>
          <Link href="/track" className="rounded-full border border-line py-3 text-center text-sm font-semibold">Track order</Link>
        </div>
      </Drawer>
    </>
  );
}
