import { percentOff, taka } from "@/lib/money";

export function Price({
  price,
  compareAt,
  from = false,
  size = "sm",
}: {
  price: number;
  compareAt?: number | null;
  from?: boolean;
  size?: "sm" | "lg";
}) {
  const off = percentOff(price, compareAt);
  return (
    <p className="flex flex-wrap items-baseline gap-x-2">
      {from && <span className="text-xs text-muted">from</span>}
      <span className={`${size === "lg" ? "text-2xl" : "text-[0.9375rem]"} font-semibold tabular-nums ${off ? "text-sale" : "text-ink"}`}>
        {taka(price)}
      </span>
      {off && compareAt ? (
        <>
          <span className={`${size === "lg" ? "text-base" : "text-xs"} text-muted line-through tabular-nums`}>{taka(compareAt)}</span>
          {size === "lg" && <span className="rounded-full bg-sale/10 px-2 py-0.5 text-xs font-semibold text-sale">Save {off}%</span>}
        </>
      ) : null}
    </p>
  );
}
