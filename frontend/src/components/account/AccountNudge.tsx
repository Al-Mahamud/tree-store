"use client";

import Link from "next/link";
import { Heart } from "lucide-react";
import { useAccount } from "./AccountProvider";

/** After a guest order: suggest an account (the order joins it when they sign up with this number). */
export function AccountNudge() {
  const { me } = useAccount();
  if (me !== null) return null;
  return (
    <div className="mt-6 flex flex-wrap items-center gap-4 rounded-[2rem] bg-forest p-6 text-bone md:p-8">
      <span className="grid h-12 w-12 place-items-center rounded-full bg-bone/10 text-lime"><Heart size={22} /></span>
      <div className="min-w-0 flex-1">
        <p className="font-display text-2xl">Keep track of your plants</p>
        <p className="text-sm text-bone/70">Create an account with this mobile number and this order joins it, with a wishlist and saved addresses.</p>
      </div>
      <Link href="/register?next=/account" className="rounded-full bg-lime px-6 py-3 text-sm font-semibold text-forest-deep">Create an account</Link>
    </div>
  );
}
