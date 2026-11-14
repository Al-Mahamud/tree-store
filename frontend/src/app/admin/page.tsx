"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { request } from "@/lib/client";
import type { Dashboard } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { day } from "@/lib/format";
import { site } from "@/config/site";
import { Card, Empty, Loading, PageHeader, Stat } from "@/components/admin/ui";
import { OrderTable } from "@/components/admin/OrderTable";
import { useAdmin } from "@/components/admin/AdminShell";

/* eslint-disable @next/next/no-img-element */
export default function DashboardPage() {
  const { staff } = useAdmin();
  const [data, setData] = useState<Dashboard | null>(null);
  useEffect(() => {
    const load = () => request<Dashboard>("GET", "/admin/dashboard").then(setData).catch(() => undefined);
    void load();
    const timer = setInterval(load, 60_000);
    return () => clearInterval(timer);
  }, []);
  if (!data) return <Loading />;
  const best = Math.max(1, ...data.week.map((d) => d.revenue));
  const hour = new Date().getHours();
  const hello = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
  const placed = data.open.PLACED ?? 0;
  const confirmed = data.open.CONFIRMED ?? 0;

  return (
    <>
      <PageHeader title={`${hello}, ${staff.name.split(" ")[0]}`} note={`Today at ${site.name}. This page refreshes every minute.`} />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Orders today" value={data.ordersToday} note={`Average order ${taka(data.averageOrder)}`} />
        <Stat label="Sales today" value={taka(data.revenueToday)} note="Placed orders, not cancelled" />
        <Stat label="To confirm" value={placed} note="New orders: call or check stock" href="/admin/orders?filter=PLACED" tone={placed > 0 ? "warn" : "plain"} />
        <Stat label="To send" value={confirmed} note={`${data.open.SHIPPED ?? 0} on the way · ${data.awaitingPayment} awaiting payment`} href="/admin/orders?filter=CONFIRMED" />
      </div>
      {(data.refundsDue > 0 || data.lowStock > 0) && (
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {data.refundsDue > 0 && <Stat label="Refunds due" value={data.refundsDue} note="Paid online, then cancelled" href="/admin/orders?filter=REFUND_DUE" tone="warn" />}
          {data.lowStock > 0 && <Stat label="Running low" value={data.lowStock} note="Plant options at or below the low-stock level" href="/admin/stock?low=1" tone="warn" />}
        </div>
      )}

      <div className="mt-5 grid gap-4 xl:grid-cols-3">
        <Card title="Last 7 days" className="xl:col-span-2">
          <ul className="space-y-2">
            {data.week.map((d) => (
              <li key={d.date} className="grid grid-cols-[5.5rem_1fr_5.5rem] items-center gap-3 text-xs">
                <span className="font-semibold text-ink-soft">{day(d.date)}</span>
                <span className="h-6 overflow-hidden rounded-full bg-mist"><span className="block h-full rounded-full bg-moss" style={{ width: `${(d.revenue / best) * 100}%` }} /></span>
                <span className="text-right tabular-nums"><strong>{taka(d.revenue)}</strong><span className="block text-muted">{d.orders} order{d.orders === 1 ? "" : "s"}</span></span>
              </li>
            ))}
          </ul>
        </Card>
        <Card title="Best sellers" action={<span className="text-xs text-muted">Last 30 days</span>}>
          {data.topPlants.length === 0 ? <Empty>No sales yet.</Empty> : (
            <ol className="space-y-2.5">
              {data.topPlants.map((p, i) => (
                <li key={p.slug} className="flex items-center gap-3">
                  <span className="w-4 text-xs font-semibold text-muted">{i + 1}</span>
                  <span className="h-11 w-11 shrink-0 overflow-hidden rounded-2xl bg-mist">{p.image && <img src={p.image} alt="" className="h-full w-full object-cover" />}</span>
                  <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{p.name}</span><span className="text-xs text-muted">{p.quantity} sold · {taka(p.revenue)}</span></span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>

      <div className="mt-6">
        <div className="mb-3 flex items-center justify-between"><h2 className="font-display text-2xl">Latest orders</h2><Link href="/admin/orders?filter=ALL" className="text-sm font-semibold text-moss hover:text-forest">All orders →</Link></div>
        {data.recent.length === 0 ? <Empty>No orders yet.</Empty> : <OrderTable rows={data.recent} />}
      </div>
    </>
  );
}
