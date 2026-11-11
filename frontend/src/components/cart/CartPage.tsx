"use client";

import Link from "next/link";
import { ArrowRight, Lock, MapPin, Sprout } from "lucide-react";
import { taka } from "@/lib/money";
import { site } from "@/config/site";
import { useCart } from "./CartProvider";
import { CartLines } from "./CartLines";
import { CouponForm } from "./CouponForm";
import { FreeDeliveryBar } from "./FreeDeliveryBar";

export function CartPage() {
  const { cart } = useCart();
  if (!cart) return <div className="container-page min-h-[50vh] py-16"><div className="h-12 w-56 animate-pulse rounded-full bg-mist" /></div>;
  if (cart.items.length === 0) {
    return (
      <div className="container-page grid min-h-[55vh] place-items-center py-16 text-center">
        <div>
          <span className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-mist"><Sprout size={28} className="text-moss" /></span>
          <h1 className="mt-6 font-display text-5xl">Your bag is empty</h1>
          <p className="mt-3 text-sm text-muted">Every garden starts with one plant.</p>
          <div className="mt-8 flex justify-center gap-3">
            <Link href="/c/trees" className="rounded-full bg-forest px-7 py-3.5 text-sm font-semibold text-bone">Shop trees</Link>
            <Link href="/c/indoor" className="rounded-full border border-forest px-7 py-3.5 text-sm font-semibold">Indoor plants</Link>
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="container-page py-10 md:py-14">
      <h1 className="font-display text-5xl md:text-6xl">Your bag <span className="text-muted">({cart.itemCount})</span></h1>
      <div className="mt-10 grid gap-10 lg:grid-cols-12 lg:gap-14">
        <div className="space-y-3 lg:col-span-7 xl:col-span-8">
          <FreeDeliveryBar cart={cart} />
          <CartLines lines={cart.items} />
        </div>
        <aside className="lg:col-span-5 xl:col-span-4">
          <div className="space-y-5 rounded-[2rem] bg-mist p-6 md:p-8 lg:sticky lg:top-28">
            <h2 className="font-display text-3xl">Summary</h2>
            <CouponForm cart={cart} />
            <dl className="space-y-2 border-t border-line pt-5 text-sm">
              <div className="flex justify-between"><dt className="text-muted">Plants</dt><dd className="tabular-nums">{taka(cart.subtotal)}</dd></div>
              {cart.discount > 0 && <div className="flex justify-between text-ok"><dt>Discount</dt><dd className="tabular-nums">−{taka(cart.discount)}</dd></div>}
              <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="text-muted">{cart.coupon?.freeShipping || cart.remainingForFreeDelivery === 0 ? "Free" : `From ${taka(site.deliveryFees.insideDhaka)}`}</dd></div>
            </dl>
            <div className="flex items-baseline justify-between border-t border-line pt-4">
              <span className="font-semibold">Estimated total</span>
              <span className="text-2xl font-semibold tabular-nums">{taka(cart.total)}</span>
            </div>
            {cart.dhakaOnly && (
              <p className="flex gap-2 rounded-2xl bg-clay/10 p-3 text-xs text-ink-soft"><MapPin size={15} className="shrink-0 text-clay" />Your bag has a large tree that we deliver inside Dhaka only.</p>
            )}
            {cart.canCheckout ? (
              <Link href="/checkout" className="group flex w-full items-center justify-center gap-2 rounded-full bg-forest py-4 text-sm font-semibold text-bone hover:bg-forest-deep">
                Checkout <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
              </Link>
            ) : (
              <p role="alert" className="text-sm text-sale">Some plants need your attention before checkout.</p>
            )}
            <p className="flex items-center justify-center gap-1.5 text-xs text-muted"><Lock size={12} /> Secure checkout · Cash on delivery available</p>
          </div>
        </aside>
      </div>
    </div>
  );
}
