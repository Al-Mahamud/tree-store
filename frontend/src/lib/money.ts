const formatter = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 0 });

/** ৳2,490 — whole taka with Indian digit grouping (1,00,000), as used in Bangladesh. */
export function taka(amount: number): string {
  return `৳${formatter.format(amount)}`;
}

export function percentOff(price: number, compareAt: number | null | undefined): number | null {
  if (!compareAt || compareAt <= price) return null;
  return Math.round(((compareAt - price) / compareAt) * 100);
}
