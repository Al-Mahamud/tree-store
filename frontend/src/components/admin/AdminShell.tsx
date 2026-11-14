"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { BadgePercent, Boxes, ExternalLink, FolderTree, LayoutDashboard, Layers, LogOut, Menu, MessageSquareText, ShoppingBag, Sprout, Truck, Users, X } from "lucide-react";
import { request } from "@/lib/client";
import { site } from "@/config/site";
import type { Staff } from "@/lib/admin-types";

type AdminContext = { staff: Staff; notify: (message: string) => void };
const Context = createContext<AdminContext | null>(null);

export function useAdmin(): AdminContext {
  const context = useContext(Context);
  if (!context) throw new Error("useAdmin must be used inside AdminShell");
  return context;
}

const nav = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/orders", label: "Orders", icon: ShoppingBag },
  { href: "/admin/products", label: "Plants", icon: Sprout },
  { href: "/admin/stock", label: "Stock", icon: Boxes },
  { href: "/admin/categories", label: "Categories", icon: FolderTree },
  { href: "/admin/collections", label: "Collections", icon: Layers },
  { href: "/admin/coupons", label: "Coupons", icon: BadgePercent },
  { href: "/admin/delivery", label: "Delivery", icon: Truck },
  { href: "/admin/texts", label: "Text messages", icon: MessageSquareText },
  { href: "/admin/team", label: "Team", icon: Users },
];

/** The admin frame: sidebar, staff name, sign-out, and a redirect to sign-in for everyone else. */
export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [staff, setStaff] = useState<Staff | null | undefined>(undefined);
  const [menu, setMenu] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const isLogin = pathname === "/admin/login";

  useEffect(() => {
    if (isLogin) return;
    request<{ staff: Staff | null }>("GET", "/staff/me")
      .then((r) => setStaff(r.staff))
      .catch(() => setStaff(null));
  }, [isLogin]);

  useEffect(() => {
    if (staff === null && !isLogin) router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
  }, [staff, isLogin, pathname, router]);

  // eslint-disable-next-line react-hooks/set-state-in-effect -- close the phone menu after navigating.
  useEffect(() => setMenu(false), [pathname]);

  const notify = useCallback((message: string) => {
    setToast(message);
    setTimeout(() => setToast(null), 3500);
  }, []);

  if (isLogin) return <>{children}</>;
  if (!staff) return <div className="grid min-h-dvh place-items-center bg-bone text-sm text-muted" aria-busy="true">Loading the admin area…</div>;

  const active = (href: string) => (href === "/admin" ? pathname === "/admin" : pathname.startsWith(href));
  const links = (
    <ul className="space-y-1">
      {nav.map(({ href, label, icon: Icon }) => (
        <li key={href}>
          <Link href={href} aria-current={active(href) ? "page" : undefined} className={`flex items-center gap-3 rounded-full px-3.5 py-2.5 text-sm font-semibold transition ${active(href) ? "bg-lime text-forest-deep" : "text-bone/75 hover:bg-white/10 hover:text-bone"}`}>
            <Icon size={18} /> {label}
          </Link>
        </li>
      ))}
    </ul>
  );
  const footer = (
    <div className="space-y-2 border-t border-white/10 pt-4">
      <Link href="/" target="_blank" className="flex items-center gap-2 px-3 text-xs font-semibold text-bone/60 hover:text-bone"><ExternalLink size={14} /> Open the shop</Link>
      <div className="flex items-center gap-3 rounded-2xl bg-white/5 p-3">
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-lime font-display text-lg text-forest-deep">{staff.name.charAt(0)}</span>
        <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{staff.name}</span><span className="block text-xs text-bone/55">{staff.phone}</span></span>
        <button
          type="button"
          aria-label="Sign out"
          onClick={async () => {
            await request("POST", "/staff/logout").catch(() => undefined);
            router.replace("/admin/login");
          }}
          className="grid h-9 w-9 place-items-center rounded-full text-bone/70 hover:bg-white/10 hover:text-bone"
        >
          <LogOut size={17} />
        </button>
      </div>
    </div>
  );

  return (
    <Context.Provider value={{ staff, notify }}>
      <div className="min-h-dvh bg-bone lg:grid lg:grid-cols-[15.5rem_1fr]">
        <aside className="sticky top-0 hidden h-dvh flex-col gap-6 bg-forest-deep p-4 text-bone lg:flex print:hidden">
          <Brand />
          <nav aria-label="Admin" className="flex-1">{links}</nav>
          {footer}
        </aside>
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between bg-forest-deep px-4 text-bone lg:hidden print:hidden">
          <Brand />
          <button type="button" aria-label="Menu" onClick={() => setMenu(true)} className="grid h-10 w-10 place-items-center rounded-full bg-white/10"><Menu size={20} /></button>
        </header>
        {menu && (
          <div className="fixed inset-0 z-50 bg-forest-deep/50 lg:hidden" onClick={() => setMenu(false)}>
            <div className="flex h-full w-72 flex-col gap-6 overflow-y-auto bg-forest-deep p-4 text-bone" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between"><Brand /><button type="button" aria-label="Close" onClick={() => setMenu(false)} className="grid h-9 w-9 place-items-center rounded-full bg-white/10"><X size={18} /></button></div>
              <nav aria-label="Admin" className="flex-1">{links}</nav>
              {footer}
            </div>
          </div>
        )}
        <main className="min-w-0 px-4 py-6 md:px-8 md:py-8">{children}</main>
      </div>
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-6 z-[70] flex justify-center px-4 print:hidden">
        {toast && <p className="animate-rise rounded-full bg-forest-deep px-5 py-3 text-sm font-medium text-bone shadow-xl">{toast}</p>}
      </div>
    </Context.Provider>
  );
}

function Brand() {
  return (
    <Link href="/admin" className="flex items-center gap-2.5">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden>
        <circle cx="16" cy="16" r="16" className="fill-lime" />
        <path d="M16 25V13" className="stroke-forest-deep" strokeWidth="1.8" strokeLinecap="round" />
        <path d="M16 15c0-4 3-7 7-7 0 4-3 7-7 7Z" className="fill-forest-deep" />
        <path d="M16 19c0-3-2.5-5.5-6-5.5 0 3 2.5 5.5 6 5.5Z" className="fill-forest-deep" opacity=".8" />
      </svg>
      <span className="font-display text-2xl leading-none">{site.name}</span>
      <span className="kicker rounded-full bg-white/10 px-2 py-0.5 text-[0.65rem] text-lime">admin</span>
    </Link>
  );
}
