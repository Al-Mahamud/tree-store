import Link from "next/link";
import { ArrowUpRight } from "lucide-react";

export function SectionHeading({
  kicker,
  title,
  href,
  linkLabel = "See all",
  tone = "light",
}: {
  kicker?: string;
  title: React.ReactNode;
  href?: string;
  linkLabel?: string;
  tone?: "light" | "dark";
}) {
  return (
    <div className="mb-8 flex items-end justify-between gap-6 md:mb-10">
      <div>
        {kicker && <p className={`kicker mb-3 ${tone === "dark" ? "text-lime" : "text-moss"}`}>{kicker}</p>}
        <h2 className="font-display text-4xl leading-[1.05] md:text-5xl">{title}</h2>
      </div>
      {href && (
        <Link
          href={href}
          className={`group inline-flex shrink-0 items-center gap-1.5 rounded-full border px-4 py-2 text-sm font-semibold transition ${
            tone === "dark" ? "border-bone/30 hover:bg-bone hover:text-forest-deep" : "border-line hover:border-forest hover:bg-forest hover:text-bone"
          }`}
        >
          {linkLabel} <ArrowUpRight size={15} className="transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
        </Link>
      )}
    </div>
  );
}
