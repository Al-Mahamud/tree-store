import Link from "next/link";
import type { PaymentMethod, PaymentStatus } from "@/lib/admin-types";

export const btn = {
  primary: "inline-flex items-center justify-center gap-1.5 rounded-full bg-forest px-4 py-2.5 text-sm font-semibold text-bone transition hover:bg-forest-deep disabled:cursor-not-allowed disabled:opacity-50",
  lime: "inline-flex items-center justify-center gap-1.5 rounded-full bg-lime px-4 py-2.5 text-sm font-semibold text-forest-deep transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-50",
  ghost: "inline-flex items-center justify-center gap-1.5 rounded-full border border-line bg-white px-4 py-2 text-sm font-semibold transition hover:border-forest/50 disabled:cursor-not-allowed disabled:opacity-50",
  danger: "inline-flex items-center justify-center gap-1.5 rounded-full bg-sale px-4 py-2.5 text-sm font-semibold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50",
  icon: "grid h-9 w-9 shrink-0 place-items-center rounded-full border border-line bg-white text-ink-soft transition hover:border-forest/50 hover:text-ink disabled:cursor-not-allowed disabled:opacity-40",
};

export const input = "w-full rounded-xl border border-line bg-white px-3 py-2 text-sm outline-none transition focus:border-forest focus:ring-4 focus:ring-lime/40";

export function PageHeader({ title, note, children }: { title: string; note?: React.ReactNode; children?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="font-display text-4xl leading-none tracking-tight">{title}</h1>
        {note && <p className="mt-1.5 text-sm text-muted">{note}</p>}
      </div>
      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </div>
  );
}

export function Card({ title, action, className = "", children }: { title?: React.ReactNode; action?: React.ReactNode; className?: string; children: React.ReactNode }) {
  return (
    <section className={`rounded-3xl border border-line bg-white p-4 md:p-5 ${className}`}>
      {(title || action) && (
        <div className="mb-3 flex items-center justify-between gap-3">
          {title && <h2 className="font-display text-xl">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function Stat({ label, value, note, href, tone = "plain" }: { label: string; value: React.ReactNode; note?: string; href?: string; tone?: "plain" | "warn" | "good" }) {
  const toneClass = tone === "warn" ? "border-clay/40 bg-clay/10" : tone === "good" ? "border-moss/40 bg-sage/40" : "border-line bg-white";
  const body = (
    <>
      <p className="kicker text-muted">{label}</p>
      <p className="mt-1 font-display text-4xl tabular-nums">{value}</p>
      {note && <p className="mt-0.5 text-xs text-muted">{note}</p>}
    </>
  );
  return href ? (
    <Link href={href} className={`block rounded-3xl border p-4 transition hover:border-forest/50 ${toneClass}`}>{body}</Link>
  ) : (
    <div className={`rounded-3xl border p-4 ${toneClass}`}>{body}</div>
  );
}

export function Empty({ children }: { children: React.ReactNode }) {
  return <p className="rounded-3xl border border-dashed border-line bg-white px-4 py-12 text-center text-sm text-muted">{children}</p>;
}

export function Loading() {
  return <div className="h-64 animate-pulse rounded-3xl bg-white" aria-busy="true" />;
}

export function PaymentBadge({ method, status }: { method: PaymentMethod; status: PaymentStatus }) {
  const text = status === "REFUNDED" ? "Refunded" : status === "PAID" ? (method === "COD" ? "Cash paid" : "Paid online") : method === "COD" ? "Cash due" : "Unpaid";
  const tone = status === "PAID" ? "bg-ok/15 text-ok" : status === "REFUNDED" ? "bg-mist text-ink-soft" : method === "COD" ? "bg-bone text-ink-soft ring-1 ring-line" : "bg-sale/10 text-sale";
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${tone}`}>{text}</span>;
}

export function ZoneBadge({ zone, dhakaOnly }: { zone: "DHAKA" | "OUTSIDE"; dhakaOnly?: boolean }) {
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${zone === "DHAKA" ? "bg-sage/60 text-forest-deep" : "bg-clay/15 text-clay"}`}>
      {zone === "DHAKA" ? (dhakaOnly ? "Dhaka · large tree" : "Dhaka van") : "Courier"}
    </span>
  );
}

export function Pager({ page, pageSize, total, onPage }: { page: number; pageSize: number; total: number; onPage: (page: number) => void }) {
  if (total <= pageSize) return null;
  return (
    <div className="mt-4 flex items-center justify-center gap-3 text-sm">
      <button type="button" className={btn.ghost} disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span className="text-muted">Page {page + 1} of {Math.ceil(total / pageSize)}</span>
      <button type="button" className={btn.ghost} disabled={(page + 1) * pageSize >= total} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  );
}

/** Tabs that filter a list, with an optional count bubble. */
export function FilterTabs<T extends string>({ tabs, value, counts, onChange, alert }: { tabs: { value: T; label: string; count?: string }[]; value: T; counts?: Record<string, number>; onChange: (value: T) => void; alert?: string }) {
  return (
    <div className="no-scrollbar -mx-4 mb-3 flex gap-1.5 overflow-x-auto px-4 md:mx-0 md:flex-wrap md:px-0">
      {tabs.map((t) => {
        const count = t.count ? counts?.[t.count] : undefined;
        const on = value === t.value;
        return (
          <button key={t.value} type="button" onClick={() => onChange(t.value)} aria-pressed={on} className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-3.5 py-2 text-sm font-semibold transition ${on ? "bg-forest text-bone" : "bg-white text-ink-soft ring-1 ring-line hover:text-ink"}`}>
            {t.label}
            {count ? <span className={`rounded-full px-1.5 text-xs ${on ? "bg-lime text-forest-deep" : t.value === alert ? "bg-sale text-white" : "bg-mist"}`}>{count}</span> : null}
          </button>
        );
      })}
    </div>
  );
}
