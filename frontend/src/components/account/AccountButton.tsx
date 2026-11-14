"use client";

import Link from "next/link";
import { Heart, User } from "lucide-react";
import { useAccount } from "./AccountProvider";
import { useWishlist } from "./WishlistProvider";

/** Header icons: account (the customer's initial once signed in) and the wishlist with its count. */
export function AccountButton() {
  const { me } = useAccount();
  const { ids } = useWishlist();
  return (
    <>
      <Link href={me ? "/account" : "/login"} aria-label={me ? "Your account" : "Sign in"} className="hidden h-10 w-10 place-items-center rounded-full hover:bg-mist sm:grid">
        {me ? <span className="grid h-8 w-8 place-items-center rounded-full bg-forest font-display text-lg text-lime">{me.name.trim().charAt(0).toUpperCase()}</span> : <User size={20} strokeWidth={1.7} />}
      </Link>
      <Link href="/account/wishlist" aria-label={`Wishlist${ids.length ? `, ${ids.length} saved` : ""}`} className="relative hidden h-10 w-10 place-items-center rounded-full hover:bg-mist sm:grid">
        <Heart size={20} strokeWidth={1.7} />
        {ids.length > 0 && <span className="absolute right-0.5 top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-clay px-1 text-[0.625rem] font-bold text-white">{ids.length}</span>}
      </Link>
    </>
  );
}
