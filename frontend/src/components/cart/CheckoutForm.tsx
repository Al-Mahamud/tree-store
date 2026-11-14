"use client";

import { useDeliveryFees } from "@/lib/useDeliveryFees";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Banknote, Check, CreditCard, Lock, MapPin, Plus } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import { taka } from "@/lib/money";
import { site } from "@/config/site";
import type { PaymentOptions, Quote, SavedAddress, Zone } from "@/lib/types";
import { useAccount } from "@/components/account/AccountProvider";
import { Field } from "@/components/ui/Field";
import { useCart } from "./CartProvider";

type Form = { name: string; phone: string; email: string; address: string; area: string; city: string; zone: Zone; note: string; paymentMethod: "COD" | "ONLINE" };

const SAVED = "shobuj.checkout";
const empty: Form = { name: "", phone: "", email: "", address: "", area: "", city: "Dhaka", zone: "DHAKA", note: "", paymentMethod: "COD" };

/* eslint-disable @next/next/no-img-element */
export function CheckoutForm() {
  const { cart, refresh } = useCart();
  const fees = useDeliveryFees();
  const router = useRouter();
  const [form, setForm] = useState<Form>(empty);
  const [quote, setQuote] = useState<Quote | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);
  const { me } = useAccount();
  const [saved, setSaved] = useState<SavedAddress[]>([]);
  const [chosen, setChosen] = useState<number | "new">("new");
  const [saveAddress, setSaveAddress] = useState(true);
  const [careTexts, setCareTexts] = useState(true);
  const [payOptions, setPayOptions] = useState<PaymentOptions | null>(null);
  useEffect(() => {
    request<PaymentOptions>("GET", "/payments/options").then(setPayOptions).catch(() => setPayOptions(null));
  }, []);

  // Signed in: start from the default saved address, or at least the customer's name and number.
  useEffect(() => {
    if (!me) return;
    request<SavedAddress[]>("GET", "/account/addresses")
      .then((list) => {
        setSaved(list);
        const first = list.find((a) => a.isDefault) ?? list[0];
        if (first) {
          setChosen(first.id);
          setForm((f) => ({ ...f, ...fromSaved(first), email: f.email || me.email || "" }));
        } else {
          setForm((f) => ({ ...f, name: f.name || me.name, phone: f.phone || me.phone, email: f.email || me.email || "" }));
        }
      })
      .catch(() => setSaved([]));
  }, [me]);

  const choose = (a: SavedAddress | "new") => {
    setErrors({});
    if (a === "new") {
      setChosen("new");
      setForm((f) => ({ ...f, address: "", area: "", city: "Dhaka", zone: "DHAKA", note: "" }));
    } else {
      setChosen(a.id);
      setForm((f) => ({ ...f, ...fromSaved(a) }));
    }
  };

  useEffect(() => {
    try {
      const saved = localStorage.getItem(SAVED);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- restoring saved form values once.
      if (saved) setForm((f) => ({ ...f, ...JSON.parse(saved), note: "", paymentMethod: "COD" }));
    } catch {
      // Storage may be unavailable; the form starts empty.
    }
  }, []);

  useEffect(() => {
    if (!cart || cart.items.length === 0) return;
    let cancelled = false;
    request<Quote>("POST", "/checkout/quote", { zone: form.zone })
      .then((q) => !cancelled && setQuote(q))
      .catch(() => !cancelled && setQuote(null));
    return () => {
      cancelled = true;
    };
  }, [form.zone, cart]);

  const set = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) {
      setErrors((current) => {
        const next = { ...current };
        delete next[key];
        return next;
      });
    }
  };

  if (cart && cart.items.length === 0 && !placing) {
    return (
      <div className="container-page grid min-h-[60vh] place-items-center text-center">
        <div>
          <h1 className="font-display text-4xl">Your bag is empty</h1>
          <Link href="/shop" className="mt-6 inline-block rounded-full bg-forest px-8 py-3.5 text-sm font-semibold text-bone">Browse plants</Link>
        </div>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPlacing(true);
    setFormError(null);
    setErrors({});
    try {
      const placed = await request<{ number: string; token: string; paymentUrl: string | null }>("POST", "/checkout", {
        ...form,
        saveAddress: Boolean(me) && chosen === "new" && saveAddress,
        careTexts,
      });
      try {
        const { name, phone, email, address, area, city, zone } = form;
        localStorage.setItem(SAVED, JSON.stringify({ name, phone, email, address, area, city, zone }));
      } catch {
        // Not remembering details is fine.
      }
      if (placed.paymentUrl) {
        window.location.href = placed.paymentUrl;
        return;
      }
      router.replace(`/order/${placed.number}?t=${encodeURIComponent(placed.token)}`);
      void refresh();
    } catch (err) {
      setPlacing(false);
      if (err instanceof ApiError) {
        setErrors(err.fields);
        setFormError(err.message);
        if (err.code.startsWith("checkout.") || err.code.startsWith("cart.")) void refresh();
      } else {
        setFormError("Something went wrong. Please try again.");
      }
    }
  };

  const blocked = Boolean(quote?.problem);

  return (
    <form onSubmit={submit} noValidate className="container-page grid gap-10 pb-16 pt-4 lg:grid-cols-12 lg:gap-14 md:pt-8">
      <div className="space-y-6 lg:col-span-7">
        <h1 className="font-display text-5xl md:text-6xl">Checkout</h1>
        <Step n={1} title="Your details">
          {me === null && (
            <p className="mb-5 rounded-2xl bg-mist px-4 py-3 text-sm">
              Have an account? <Link href="/login?next=/checkout" className="font-semibold text-moss underline-offset-4 hover:underline">Sign in</Link> to use your saved addresses.
            </p>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full name" error={errors.name} className="sm:col-span-2"><input autoComplete="name" value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
            <Field label="Mobile number" hint="We call to confirm and the rider calls on arrival." error={errors.phone}><input type="tel" inputMode="tel" autoComplete="tel" placeholder="01XXXXXXXXX" value={form.phone} onChange={(e) => set("phone", e.target.value)} /></Field>
            <Field label="Email (optional)" hint="For your receipt and care guide." error={errors.email}><input type="email" autoComplete="email" value={form.email} onChange={(e) => set("email", e.target.value)} /></Field>
          </div>
        </Step>

        <Step n={2} title="Delivery">
          {saved.length > 0 && (
            <div className="mb-5 grid gap-2 sm:grid-cols-2" role="radiogroup" aria-label="Saved addresses">
              {saved.map((a) => (
                <button key={a.id} type="button" role="radio" aria-checked={chosen === a.id} onClick={() => choose(a)} className={`relative rounded-[1.25rem] border p-4 text-left transition ${chosen === a.id ? "border-forest bg-white" : "border-line hover:border-forest/50"}`}>
                  <span className="block text-sm font-semibold">{a.label}{a.isDefault && <span className="ml-1.5 text-xs font-normal text-muted">· default</span>}</span>
                  <span className="block truncate text-xs text-ink-soft">{a.address}, {a.area}, {a.city}</span>
                  <span className="block text-xs text-muted">{a.zone === "DHAKA" ? "Inside Dhaka" : "Outside Dhaka"} · {a.name}</span>
                  {chosen === a.id && <Check size={16} className="absolute right-4 top-4 text-forest" />}
                </button>
              ))}
              <button type="button" role="radio" aria-checked={chosen === "new"} onClick={() => choose("new")} className={`flex items-center gap-2 rounded-[1.25rem] border border-dashed p-4 text-sm font-semibold transition ${chosen === "new" ? "border-forest bg-white" : "border-line hover:border-forest/50"}`}>
                <Plus size={16} /> Deliver somewhere else
              </button>
            </div>
          )}
          {chosen !== "new" && quote?.problem && (
            <p role="alert" className="mb-3 flex gap-2 rounded-[1.25rem] bg-clay/10 p-4 text-sm text-ink-soft"><MapPin size={17} className="mt-0.5 shrink-0 text-clay" />{quote.problem}</p>
          )}
          {chosen !== "new" && Object.values(errors).some(Boolean) && (
            <p className="mb-3 text-xs text-sale">{Object.values(errors).find(Boolean)} <button type="button" className="underline" onClick={() => setChosen("new")}>Edit the address</button></p>
          )}
          <div className={chosen !== "new" ? "hidden" : ""}>
          <fieldset>
            <legend className="mb-3 text-sm font-semibold">Where should we deliver?</legend>
            <div className="grid gap-3 sm:grid-cols-2">
              {([["DHAKA", "Inside Dhaka", "1–2 days, our own van", fees.insideDhaka], ["OUTSIDE", "Outside Dhaka", "2–4 days, by courier", fees.outsideDhaka]] as const).map(([value, label, time, price]) => (
                <label key={value} className={`flex cursor-pointer items-start gap-3 rounded-[1.25rem] border p-4 transition ${form.zone === value ? "border-forest bg-white" : "border-line hover:border-forest/50"}`}>
                  <input type="radio" name="zone" value={value} checked={form.zone === value} onChange={() => set("zone", value)} className="mt-1 accent-forest" />
                  <span className="flex-1"><span className="block text-sm font-semibold">{label}</span><span className="block text-xs text-muted">{time}</span></span>
                  <span className="text-sm font-semibold tabular-nums">{taka(price)}</span>
                </label>
              ))}
            </div>
            {quote?.problem && (
              <p role="alert" className="mt-3 flex gap-2 rounded-[1.25rem] bg-clay/10 p-4 text-sm text-ink-soft"><MapPin size={17} className="mt-0.5 shrink-0 text-clay" />{quote.problem}</p>
            )}
          </fieldset>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Address" hint="House, road, block or village" error={errors.address} className="sm:col-span-2"><input autoComplete="street-address" value={form.address} onChange={(e) => set("address", e.target.value)} /></Field>
            <Field label="Area / thana" error={errors.area}><input autoComplete="address-level3" value={form.area} onChange={(e) => set("area", e.target.value)} /></Field>
            <Field label="City / district" error={errors.city}><input autoComplete="address-level2" value={form.city} onChange={(e) => set("city", e.target.value)} /></Field>
            <Field label="Note for the delivery team (optional)" error={errors.note} className="sm:col-span-2"><textarea rows={2} value={form.note} onChange={(e) => set("note", e.target.value)} placeholder="e.g. Rooftop delivery, please call from the gate" /></Field>
            {me && (
              <label className="flex items-center gap-2.5 text-sm font-semibold sm:col-span-2">
                <input type="checkbox" checked={saveAddress} onChange={(e) => setSaveAddress(e.target.checked)} className="h-4 w-4 accent-forest" /> Save this address to my account
              </label>
            )}
          </div>
          </div>
        </Step>

        <Step n={3} title="Payment">
          <div className="grid gap-3">
            <label className={`flex cursor-pointer items-center gap-4 rounded-[1.25rem] border p-4 ${form.paymentMethod === "COD" ? "border-forest bg-white" : "border-line"}`}>
              <input type="radio" name="payment" checked={form.paymentMethod === "COD"} onChange={() => set("paymentMethod", "COD")} className="accent-forest" />
              <Banknote size={22} strokeWidth={1.5} className="text-moss" />
              <span className="flex-1"><span className="block text-sm font-semibold">Cash on delivery</span><span className="block text-xs text-muted">Check your plants, then pay the delivery team.</span></span>
            </label>
            <label className={`flex items-center gap-4 rounded-[1.25rem] border p-4 ${!payOptions?.online ? "cursor-not-allowed border-line opacity-60" : form.paymentMethod === "ONLINE" ? "cursor-pointer border-forest bg-white" : "cursor-pointer border-line"}`}>
              <input type="radio" name="payment" disabled={!payOptions?.online} checked={form.paymentMethod === "ONLINE"} onChange={() => set("paymentMethod", "ONLINE")} className="accent-forest" />
              <CreditCard size={22} strokeWidth={1.5} className={payOptions?.online ? "text-moss" : ""} />
              <span className="flex-1"><span className="block text-sm font-semibold">Card, bKash or Nagad</span><span className="block text-xs text-muted">{payOptions?.online ? "Pay now on the secure SSLCommerz page." : "Not available right now."}</span></span>
            </label>
            {form.paymentMethod === "ONLINE" && payOptions && (
              <p className="rounded-2xl bg-mist px-4 py-3 text-xs text-ink-soft">After you place the order you&rsquo;ll pay on SSLCommerz. Your plants are set aside for {payOptions.holdMinutes} minutes; if the payment doesn&rsquo;t go through you can try again or switch to cash.</p>
            )}
            <label className="mt-2 flex items-start gap-3 rounded-[1.25rem] bg-mist p-4 text-sm">
              <input type="checkbox" checked={careTexts} onChange={(e) => setCareTexts(e.target.checked)} className="mt-0.5 h-4 w-4 accent-forest" />
              <span><span className="font-semibold">Text me care tips after delivery</span><span className="block text-xs text-muted">Three short texts in the first month. Turn them off any time on your order page.</span></span>
            </label>
          </div>
        </Step>
      </div>

      <aside className="lg:col-span-5">
        <div className="rounded-[2rem] bg-mist p-6 md:p-8 lg:sticky lg:top-6">
          <h2 className="font-display text-3xl">Your order</h2>
          {!cart ? <div className="mt-6 h-32 animate-pulse rounded-2xl bg-white/60" /> : (
            <ul className="mt-5 space-y-4">
              {cart.items.map((line) => (
                <li key={line.variantId} className="flex gap-3">
                  <div className="relative w-16 shrink-0">
                    <div className="aspect-[4/5] overflow-hidden rounded-xl bg-white">{line.image && <img src={line.image} alt="" className="h-full w-full object-cover" />}</div>
                    <span className="absolute -right-2 -top-2 grid h-5 min-w-5 place-items-center rounded-full bg-forest px-1 text-[0.625rem] font-bold text-bone">{line.quantity}</span>
                  </div>
                  <div className="min-w-0 flex-1 text-sm">
                    <p className="font-semibold leading-snug">{line.name}</p>
                    <p className="text-xs text-muted">{line.size}, {line.height} · {line.planter}</p>
                  </div>
                  <p className="text-sm font-semibold tabular-nums">{taka(line.lineTotal)}</p>
                </li>
              ))}
            </ul>
          )}
          <dl className="mt-6 space-y-2 border-t border-line pt-5 text-sm">
            <Row label="Plants" value={quote ? taka(quote.subtotal) : "—"} />
            {quote && quote.discount > 0 && <Row label={`Discount (${quote.couponCode})`} value={`−${taka(quote.discount)}`} ok />}
            <Row label="Delivery" value={quote ? (quote.shippingFee === 0 ? "Free" : taka(quote.shippingFee)) : "—"} />
          </dl>
          <div className="mt-4 flex items-baseline justify-between border-t border-line pt-4">
            <span className="font-semibold">Total</span>
            <span className="text-3xl font-semibold tabular-nums">{quote ? taka(quote.total) : "—"}</span>
          </div>
          {formError && <p role="alert" className="mt-5 rounded-2xl bg-sale/10 px-4 py-3 text-sm text-sale">{formError}</p>}
          <button type="submit" disabled={placing || blocked || !cart?.canCheckout} className="mt-6 flex w-full items-center justify-center gap-2 rounded-full bg-forest py-4 text-sm font-semibold text-bone transition hover:bg-forest-deep disabled:cursor-not-allowed disabled:opacity-50">
            <Lock size={15} /> {placing ? (form.paymentMethod === "ONLINE" ? "Opening payment…" : "Placing your order…") : `${form.paymentMethod === "ONLINE" ? "Place order & pay" : "Place order"}${quote ? ` · ${taka(quote.total)}` : ""}`}
          </button>
          <p className="mt-3 text-center text-xs text-muted">Covered by our {site.guaranteeDays}-day healthy-plant guarantee.</p>
        </div>
      </aside>
    </form>
  );
}

function Step({ n, title, children }: { n: number; title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-[2rem] bg-white/50 p-5 md:p-7">
      <h2 className="mb-5 flex items-center gap-3 font-display text-2xl">
        <span className="grid h-8 w-8 place-items-center rounded-full bg-forest font-sans text-sm font-semibold text-bone">{n}</span>{title}
      </h2>
      {children}
    </section>
  );
}

function Row({ label, value, ok }: { label: string; value: string; ok?: boolean }) {
  return <div className={`flex justify-between ${ok ? "text-ok" : ""}`}><dt className={ok ? "" : "text-muted"}>{label}</dt><dd className="tabular-nums">{value}</dd></div>;
}

function fromSaved(a: SavedAddress) {
  return { name: a.name, phone: a.phone, address: a.address, area: a.area, city: a.city, zone: a.zone, note: a.note ?? "" };
}
