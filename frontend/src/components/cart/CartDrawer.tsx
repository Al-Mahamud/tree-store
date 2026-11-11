"use client";

import Link from "next/link";
import { Sprout, X } from "lucide-react";
import { Drawer } from "@/components/ui/Drawer";
import { taka } from "@/lib/money";
import { useCart } from "./CartProvider";
import { CartLines } from "./CartLines";
import { FreeDeliveryBar } from "./FreeDeliveryBar";

export function CartDrawer() {
  const { cart, open, setOpen } = useCart();
  const empty = !cart || cart.items.length === 0;
  return (
    <Drawer open={open} onClose={() => setOpen(false)} label="Your bag">
      <div className="flex h-16 shrink-0 items-center justify-between px-5">
        <p className="font-display text-2xl">Your bag {cart && cart.itemCount > 0 && <span className="text-muted">({cart.itemCount})</span>}</p>
        <button type="button" aria-label="Close bag" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-full bg-mist"><X size={18} /></button>
      </div>
      {empty ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <span className="grid h-16 w-16 place-items-center rounded-full bg-mist"><Sprout size={28} strokeWidth={1.5} className="text-moss" /></span>
          <p className="font-display text-3xl">Nothing growing here yet</p>
          <p className="text-sm text-muted">Start with an easy-care plant — they forgive almost anything.</p>
          <Link href="/shop?care=easy" onClick={() => setOpen(false)} className="mt-2 rounded-full bg-forest px-7 py-3 text-sm font-semibold text-bone">Shop easy-care plants</Link>
        </div>
      ) : (
        <>
          <div className="flex-1 space-y-3 overflow-y-auto px-5 pb-4">
            <FreeDeliveryBar cart={cart} />
            <CartLines lines={cart.items} compact />
          </div>
          <div className="shrink-0 space-y-3 border-t border-line p-5">
            {cart.discount > 0 && <div className="flex justify-between text-sm text-ok"><span>Discount ({cart.coupon?.code})</span><span className="tabular-nums">−{taka(cart.discount)}</span></div>}
            <div className="flex items-baseline justify-between">
              <span className="text-sm text-muted">Total before delivery</span>
              <span className="text-xl font-semibold tabular-nums">{taka(cart.total)}</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Link href="/cart" onClick={() => setOpen(false)} className="rounded-full border border-forest py-3.5 text-center text-sm font-semibold">View bag</Link>
              <Link href={cart.canCheckout ? "/checkout" : "/cart"} onClick={() => setOpen(false)} className="rounded-full bg-forest py-3.5 text-center text-sm font-semibold text-bone hover:bg-forest-deep">Checkout</Link>
            </div>
          </div>
        </>
      )}
    </Drawer>
  );
}
