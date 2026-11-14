import type { OrderView } from "@/lib/types";

const styles: Record<OrderView["status"], { label: string; className: string }> = {
  PENDING_PAYMENT: { label: "Awaiting payment", className: "bg-mist text-ink-soft" },
  PLACED: { label: "Placed", className: "bg-lime/60 text-forest-deep" },
  CONFIRMED: { label: "Confirmed", className: "bg-sage text-forest-deep" },
  SHIPPED: { label: "On the way", className: "bg-forest text-bone" },
  DELIVERED: { label: "Delivered", className: "bg-ok/15 text-ok" },
  CANCELLED: { label: "Cancelled", className: "bg-sale/10 text-sale" },
};

export function StatusPill({ status }: { status: OrderView["status"] }) {
  const s = styles[status];
  return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${s.className}`}>{s.label}</span>;
}
