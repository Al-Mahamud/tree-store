import Link from "next/link";
import type { AdminRow } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { ago, dateTime } from "@/lib/format";
import { StatusPill } from "@/components/account/StatusPill";
import { PaymentBadge, ZoneBadge } from "./ui";

const waiting = new Set(["PLACED", "CONFIRMED"]);

/** Orders as rows on wide screens and as cards on phones. */
export function OrderTable({ rows }: { rows: AdminRow[] }) {
  return (
    <>
      <div className="hidden overflow-hidden rounded-3xl border border-line bg-white md:block">
        <table className="w-full text-sm">
          <thead className="bg-mist/60 text-left text-xs uppercase tracking-wide text-muted">
            <tr>
              <th className="px-4 py-3 font-semibold">Order</th>
              <th className="px-4 py-3 font-semibold">Customer</th>
              <th className="px-4 py-3 font-semibold">Delivery</th>
              <th className="px-4 py-3 font-semibold">Status</th>
              <th className="px-4 py-3 font-semibold">Payment</th>
              <th className="px-4 py-3 text-right font-semibold">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {rows.map((o) => (
              <tr key={o.number} className="hover:bg-bone/70">
                <td className="px-4 py-3">
                  <Link href={`/admin/orders/${o.number}`} className="font-semibold text-ink hover:text-moss">{o.number}</Link>
                  <span className="block text-xs text-muted">{waiting.has(o.status) ? ago(o.placedAt) : dateTime(o.placedAt)}</span>
                </td>
                <td className="px-4 py-3"><span className="block font-semibold">{o.name}</span><span className="text-xs text-muted">{o.phone}</span></td>
                <td className="px-4 py-3"><span className="block">{o.area}, {o.city}</span><ZoneBadge zone={o.zone} dhakaOnly={o.dhakaOnly} /></td>
                <td className="px-4 py-3"><StatusPill status={o.status} /></td>
                <td className="px-4 py-3"><PaymentBadge method={o.paymentMethod} status={o.paymentStatus} /></td>
                <td className="px-4 py-3 text-right"><span className="font-semibold tabular-nums">{taka(o.total)}</span><span className="block text-xs text-muted">{o.itemCount} plant{o.itemCount === 1 ? "" : "s"}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul className="space-y-2 md:hidden">
        {rows.map((o) => (
          <li key={o.number}>
            <Link href={`/admin/orders/${o.number}`} className="block rounded-3xl border border-line bg-white p-4">
              <div className="flex items-center gap-2"><span className="font-semibold">{o.number}</span><StatusPill status={o.status} /><span className="ml-auto font-semibold tabular-nums">{taka(o.total)}</span></div>
              <p className="mt-1 text-sm">{o.name} · {o.area}, {o.city}</p>
              <p className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs text-muted">{ago(o.placedAt)} <ZoneBadge zone={o.zone} dhakaOnly={o.dhakaOnly} /> <PaymentBadge method={o.paymentMethod} status={o.paymentStatus} /></p>
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
