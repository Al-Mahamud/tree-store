"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ApiError, request } from "@/lib/client";
import { useAccount } from "./AccountProvider";

type WishlistContext = {
  /** Saved product ids; empty when signed out. */
  ids: number[];
  saved: (productId: number) => boolean;
  /** Saves or unsaves. Signed-out shoppers are sent to sign in first. */
  toggle: (productId: number, name?: string) => Promise<void>;
  message: string | null;
};

const Context = createContext<WishlistContext | null>(null);

/** Which plants the customer saved, shared by every heart on the page. */
export function WishlistProvider({ children }: { children: React.ReactNode }) {
  const { me } = useAccount();
  const router = useRouter();
  const pathname = usePathname();
  const [ids, setIds] = useState<number[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!me) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- signed out: nothing is saved.
      setIds([]);
      return;
    }
    request<number[]>("GET", "/account/wishlist/ids").then(setIds).catch(() => setIds([]));
  }, [me]);

  const toggle = useCallback(
    async (productId: number, name?: string) => {
      if (me === null) {
        router.push(`/login?next=${encodeURIComponent(pathname)}`);
        return;
      }
      if (!me) return;
      const wasSaved = ids.includes(productId);
      setIds((current) => (wasSaved ? current.filter((id) => id !== productId) : [productId, ...current]));
      try {
        setIds(await request<number[]>(wasSaved ? "DELETE" : "POST", wasSaved ? `/account/wishlist/${productId}` : "/account/wishlist", wasSaved ? undefined : { productId }));
        setMessage(wasSaved ? `${name ?? "Plant"} removed from your wishlist` : `${name ?? "Plant"} saved to your wishlist`);
        setTimeout(() => setMessage(null), 2500);
      } catch (e) {
        setIds((current) => (wasSaved ? [productId, ...current] : current.filter((id) => id !== productId)));
        setMessage(e instanceof ApiError ? e.message : "Couldn't update your wishlist.");
      }
    },
    [me, ids, router, pathname],
  );

  const value = useMemo(() => ({ ids, saved: (id: number) => ids.includes(id), toggle, message }), [ids, toggle, message]);
  return (
    <Context.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-20 z-[70] flex justify-center px-4">
        {message && <p className="animate-rise rounded-full bg-forest-deep px-5 py-3 text-sm font-medium text-bone shadow-xl">{message}</p>}
      </div>
    </Context.Provider>
  );
}

export function useWishlist(): WishlistContext {
  const context = useContext(Context);
  if (!context) throw new Error("useWishlist must be used inside WishlistProvider");
  return context;
}
