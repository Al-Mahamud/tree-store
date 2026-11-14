"use client";

import { useEffect, useState } from "react";
import { site } from "@/config/site";
import type { DeliveryFees } from "./types";

let cached: Promise<DeliveryFees> | null = null;

/** The delivery fees the shop has set in the admin area, loaded once per page visit. */
export function useDeliveryFees(): DeliveryFees {
  const [fees, setFees] = useState<DeliveryFees>({ ...site.deliveryFees, freeOver: site.freeDeliveryOver });
  useEffect(() => {
    cached ??= fetch("/api/v1/delivery").then((r) => (r.ok ? (r.json() as Promise<DeliveryFees>) : Promise.reject(new Error(String(r.status)))));
    cached.then(setFees).catch(() => {
      cached = null;
    });
  }, []);
  return fees;
}
