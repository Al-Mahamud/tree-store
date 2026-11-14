"use client";

import { Suspense, useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import { request } from "@/lib/client";
import type { AdminOrderPage } from "@/lib/admin-types";
import { Empty, FilterTabs, Loading, PageHeader, Pager, btn, input } from "@/components/admin/ui";
import { OrderTable } from "@/components/admin/OrderTable";

const tabs = [
  { value: "OPEN", label: "To do" },
  { value: "PLACED", label: "New", count: "PLACED" },
  { value: "CONFIRMED", label: "Confirmed", count: "CONFIRMED" },
  { value: "SHIPPED", label: "On the way", count: "SHIPPED" },
  { value: "PENDING_PAYMENT", label: "Awaiting payment", count: "PENDING_PAYMENT" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
  { value: "REFUND_DUE", label: "Refund due", count: "REFUND_DUE" },
  { value: "ALL", label: "All" },
];

const zones = [
  { value: "", label: "Everywhere" },
  { value: "DHAKA", label: "Inside Dhaka" },
  { value: "OUTSIDE", label: "Outside Dhaka" },
];

function Orders() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const filter = params.get("filter") ?? "OPEN";
  const zone = params.get("zone") ?? "";
  const q = params.get("q") ?? "";
  const page = Number(params.get("page") ?? "0");
  const [data, setData] = useState<AdminOrderPage | null>(null);
  const [search, setSearch] = useState(q);

  useEffect(() => {
    const query = new URLSearchParams({ filter, page: String(page) });
    if (zone) query.set("zone", zone);
    if (q) query.set("q", q);
    request<AdminOrderPage>("GET", `/admin/orders?${query}`).then(setData).catch(() => setData(null));
  }, [filter, zone, q, page]);

  const go = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    if (!("page" in changes)) next.delete("page");
    router.push(`${pathname}?${next}`);
  };

  return (
    <>
      <PageHeader title="Orders" note={filter === "OPEN" ? "Everything still to confirm, send or deliver, oldest first." : undefined} />
      <FilterTabs tabs={tabs} value={filter} counts={data?.counts} onChange={(v) => go({ filter: v })} alert="REFUND_DUE" />
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); go({ q: search.trim() }); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Search orders</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Order number, phone or name" className={`${input} pl-9`} /></label>
        <label className="flex items-center gap-2 text-sm"><span className="sr-only">Delivery</span>
          <select value={zone} onChange={(e) => go({ zone: e.target.value })} className={`${input} w-auto!`}>
            {zones.map((z) => <option key={z.value} value={z.value}>{z.label}</option>)}
          </select>
        </label>
        <button type="submit" className={btn.primary}>Search</button>
        {(q || zone) && <button type="button" onClick={() => { setSearch(""); go({ q: "", zone: "" }); }} className={btn.ghost}>Clear</button>}
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No orders here.</Empty> : <OrderTable rows={data.items} />}
      {data && <Pager page={page} pageSize={data.pageSize} total={data.total} onPage={(p) => go({ page: String(p) })} />}
    </>
  );
}

export default function OrdersPage() {
  return <Suspense fallback={<Loading />}><Orders /></Suspense>;
}
