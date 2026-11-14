"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { Profile } from "@/lib/types";
import { request } from "@/lib/client";

type AccountContext = {
  /** null when signed out; undefined until we know. */
  me: Profile | null | undefined;
  setMe: (me: Profile | null) => void;
  signOut: () => Promise<void>;
};

const Context = createContext<AccountContext | null>(null);

/** Who is signed in, read once from the backend session and shared by every page. */
export function AccountProvider({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<Profile | null | undefined>(undefined);

  useEffect(() => {
    request<{ customer: Profile | null }>("GET", "/auth/me")
      .then((r) => setMe(r.customer))
      .catch(() => setMe(null));
  }, []);

  const signOut = useCallback(async () => {
    await request("POST", "/auth/logout");
    setMe(null);
  }, []);

  const value = useMemo(() => ({ me, setMe, signOut }), [me, signOut]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}

export function useAccount(): AccountContext {
  const context = useContext(Context);
  if (!context) throw new Error("useAccount must be used inside AccountProvider");
  return context;
}

export function firstName(name: string): string {
  return name.trim().split(/\s+/)[0] ?? name;
}
