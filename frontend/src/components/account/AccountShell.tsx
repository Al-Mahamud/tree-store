"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Heart, LogOut, MapPin, Package, UserRound } from "lucide-react";
import { useCart } from "@/components/cart/CartProvider";
import { firstName, useAccount } from "./AccountProvider";

const tabs = [
  { href: "/account", label: "Orders", icon: Package },
  { href: "/account/wishlist", label: "Wishlist", icon: Heart },
  { href: "/account/addresses", label: "Addresses", icon: MapPin },
  { href: "/account/profile", label: "Profile", icon: UserRound },
];

/** Frame for every account page: greeting, tabs, and a redirect to sign-in for guests. */
export function AccountShell({ children }: { children: React.ReactNode }) {
  const { me, signOut } = useAccount();
  const { refresh } = useCart();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (me === null) router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [me, pathname, router]);

  if (!me) {
    return (
      <div className="container-page py-12" aria-busy="true">
        <div className="h-24 animate-pulse rounded-[2rem] bg-white/60" />
        <div className="mt-4 h-72 animate-pulse rounded-[2rem] bg-white/60" />
      </div>
    );
  }
  const active = (href: string) => (href === "/account" ? pathname === "/account" || pathname.startsWith("/account/orders") : pathname.startsWith(href));

  return (
    <div className="container-page py-8 md:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="kicker text-moss">Your account</p>
          <h1 className="mt-2 font-display text-5xl leading-none md:text-6xl">Hello, <em>{firstName(me.name)}.</em></h1>
          <p className="mt-2 text-sm text-muted">{me.phone}</p>
        </div>
        <button
          type="button"
          onClick={async () => {
            await signOut();
            await refresh();
            router.replace("/");
          }}
          className="inline-flex items-center gap-2 rounded-full border border-line px-5 py-2.5 text-sm font-semibold hover:border-forest"
        >
          <LogOut size={16} /> Sign out
        </button>
      </div>
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-[14rem_minmax(0,1fr)]">
        <nav aria-label="Account" className="min-w-0">
          <ul className="no-scrollbar -mx-4 flex gap-2 overflow-x-auto px-4 lg:mx-0 lg:flex-col lg:px-0">
            {tabs.map(({ href, label, icon: Icon }) => (
              <li key={href}>
                <Link href={href} aria-current={active(href) ? "page" : undefined} className={`flex items-center gap-2.5 whitespace-nowrap rounded-full px-5 py-3 text-sm font-semibold transition ${active(href) ? "bg-forest text-bone" : "bg-white/60 text-ink-soft hover:text-ink"}`}>
                  <Icon size={17} className={active(href) ? "text-lime" : ""} /> {label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <div className="min-w-0">{children}</div>
      </div>
    </div>
  );
}
