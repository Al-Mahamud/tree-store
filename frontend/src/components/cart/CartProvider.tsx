"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { Cart } from "@/lib/types";
import { ApiError, request } from "@/lib/client";

type CartContext = {
  cart: Cart | null;
  open: boolean;
  setOpen: (open: boolean) => void;
  busy: boolean;
  add: (variantId: number, quantity?: number) => Promise<void>;
  setQuantity: (variantId: number, quantity: number) => Promise<void>;
  remove: (variantId: number) => Promise<void>;
  applyCoupon: (code: string) => Promise<void>;
  removeCoupon: () => Promise<void>;
  refresh: () => Promise<void>;
  clear: () => void;
  /** Shows a short message at the bottom of the screen. */
  notify: (message: string) => void;
};

const Context = createContext<CartContext | null>(null);

/** Holds the bag for the whole shop. Every change returns the full bag from the server. */
export function CartProvider({ children }: { children: React.ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const notify = useCallback((message: string) => {
    setToast(message);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 4000);
  }, []);

  const run = useCallback(async (call: () => Promise<Cart>) => {
    setBusy(true);
    try {
      setCart(await call());
    } finally {
      setBusy(false);
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      setCart(await request<Cart>("GET", "/cart"));
    } catch (e) {
      if (!(e instanceof ApiError)) throw e;
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- load the bag once on start.
    void refresh();
  }, [refresh]);

  const value = useMemo<CartContext>(
    () => ({
      cart,
      open,
      setOpen,
      busy,
      refresh,
      add: async (variantId, quantity = 1) => {
        await run(() => request<Cart>("POST", "/cart/items", { variantId, quantity }));
        setOpen(true);
      },
      setQuantity: (variantId, quantity) =>
        run(() => request<Cart>("PATCH", `/cart/items/${variantId}`, { quantity })),
      remove: (variantId) => run(() => request<Cart>("DELETE", `/cart/items/${variantId}`)),
      applyCoupon: (code) => run(() => request<Cart>("POST", "/cart/coupon", { code })),
      removeCoupon: () => run(() => request<Cart>("DELETE", "/cart/coupon")),
      clear: () => setCart(null),
      notify,
    }),
    [cart, open, busy, refresh, run, notify],
  );

  return (
    <Context.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-6 z-[70] flex justify-center px-4">
        {toast && <p className="animate-rise rounded-full bg-forest-deep px-5 py-3 text-sm font-medium text-bone shadow-xl">{toast}</p>}
      </div>
    </Context.Provider>
  );
}

export function useCart(): CartContext {
  const context = useContext(Context);
  if (!context) throw new Error("useCart must be used inside CartProvider");
  return context;
}
