import { Truck } from "lucide-react";
import { taka } from "@/lib/money";
import type { Cart } from "@/lib/types";

export function FreeDeliveryBar({ cart }: { cart: Cart }) {
  const done = cart.remainingForFreeDelivery === 0;
  const progress = done ? 100 : Math.min(100, (cart.total / cart.freeDeliveryOver) * 100);
  return (
    <div className="rounded-3xl bg-mist p-4">
      <p className="flex items-center gap-2 text-sm">
        <Truck size={16} strokeWidth={1.8} className="text-moss" />
        {done ? <strong className="font-semibold">Free delivery unlocked</strong> : <span>Add <strong className="font-semibold">{taka(cart.remainingForFreeDelivery)}</strong> more for free delivery</span>}
      </p>
      <div className="mt-3 h-2 overflow-hidden rounded-full bg-white">
        <div className="h-full rounded-full bg-gradient-to-r from-moss to-lime transition-[width] duration-700" style={{ width: `${progress}%` }} />
      </div>
    </div>
  );
}
