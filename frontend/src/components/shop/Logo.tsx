import Link from "next/link";
import { site } from "@/config/site";

export function Logo({ tone = "dark" }: { tone?: "dark" | "light" }) {
  return (
    <Link href="/" aria-label={`${site.name} home`} className="inline-flex items-center gap-2">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden>
        <circle cx="16" cy="16" r="16" className={tone === "dark" ? "fill-forest" : "fill-lime"} />
        <path d="M16 25V13" className={tone === "dark" ? "stroke-lime" : "stroke-forest-deep"} strokeWidth="1.8" strokeLinecap="round" />
        <path d="M16 15c0-4 3-7 7-7 0 4-3 7-7 7Z" className={tone === "dark" ? "fill-lime" : "fill-forest-deep"} />
        <path d="M16 19c0-3-2.5-5.5-6-5.5 0 3 2.5 5.5 6 5.5Z" className={tone === "dark" ? "fill-lime" : "fill-forest-deep"} opacity=".8" />
      </svg>
      <span className="font-display text-[1.75rem] leading-none tracking-tight">{site.name}</span>
    </Link>
  );
}
