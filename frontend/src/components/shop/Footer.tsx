import Link from "next/link";
import { MessageCircle } from "lucide-react";
import { site } from "@/config/site";
import { Logo } from "./Logo";
import { NewsletterForm } from "./NewsletterForm";

const columns = [
  { title: "Shop", links: [["Fruit trees", "/c/fruit-trees"], ["Flowering trees", "/c/flowering-trees"], ["Indoor plants", "/c/indoor"], ["Bonsai", "/c/bonsai"], ["Offers", "/shop?sale=true"]] },
  { title: "Help", links: [["Track your order", "/track"], ["Delivery areas", "/pages/delivery"], ["Plant guarantee", "/pages/guarantee"], ["Planting guides", "/guides"], ["Contact", "/pages/contact"]] },
  { title: "Nursery", links: [["Our story", "/pages/about"], ["Corporate & bulk", "/pages/bulk"], ["Privacy", "/pages/privacy"], ["Terms", "/pages/terms"]] },
] as const;

export function Footer() {
  return (
    <footer className="mt-24 px-2 pb-2 md:mt-32 md:px-3 md:pb-3">
      <div className="rounded-[2rem] bg-forest-deep text-bone md:rounded-[2.5rem]">
        <div className="container-page grid gap-12 py-14 md:grid-cols-12 md:py-20">
          <div className="md:col-span-6">
            <p className="font-display text-4xl leading-[1.05] md:text-6xl">
              Grow something <em className="text-lime">that outlives you.</em>
            </p>
            <p className="mt-4 max-w-md text-sm text-bone/65">Seasonal planting reminders, care tips and first look at new arrivals. Twice a month.</p>
            <NewsletterForm />
          </div>
          <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 md:col-span-6">
            {columns.map((col) => (
              <div key={col.title}>
                <p className="kicker mb-4 text-bone/45">{col.title}</p>
                <ul className="space-y-2.5 text-sm">
                  {col.links.map(([label, href]) => (
                    <li key={href}><Link href={href} className="text-bone/85 hover:text-lime">{label}</Link></li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
        <div className="container-page flex flex-col gap-5 border-t border-bone/10 py-8 text-xs text-bone/55 md:flex-row md:items-center md:justify-between">
          <div className="text-bone"><Logo tone="light" /></div>
          <p className="flex items-center gap-2">
            <MessageCircle size={14} /> Plant doctor on WhatsApp: {site.whatsapp} · {site.address}
          </p>
          <p>© {new Date().getFullYear()} {site.name}. Grown in Bangladesh.</p>
        </div>
      </div>
    </footer>
  );
}
