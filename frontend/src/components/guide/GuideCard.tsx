import Link from "next/link";
import { ArrowUpRight, CloudRain, Droplets, Leaf, Sun, TreeDeciduous, TreePine } from "lucide-react";
import type { Guide, GuideIcon as Icon } from "@/config/guides";

const tones = {
  lime: "bg-lime text-forest-deep",
  sage: "bg-sage text-forest-deep",
  clay: "bg-clay text-bone",
  mist: "bg-mist text-forest-deep",
} as const;

export function GuideIcon({ icon, size = 28 }: { icon: Icon; size?: number }) {
  const props = { size, strokeWidth: 1.4, "aria-hidden": true };
  switch (icon) {
    case "tree": return <TreeDeciduous {...props} />;
    case "sun": return <Sun {...props} />;
    case "leaf": return <Leaf {...props} />;
    case "droplet": return <Droplets {...props} />;
    case "cloud": return <CloudRain {...props} />;
    case "bonsai": return <TreePine {...props} />;
  }
}

export function guideTone(guide: Guide): string {
  return tones[guide.tone];
}

/** A guide as a tile: colour block with its icon, title, summary and reading time. */
export function GuideCard({ guide }: { guide: Guide }) {
  return (
    <Link href={`/guides/${guide.slug}`} className="group flex h-full flex-col overflow-hidden rounded-[var(--radius-card)] border border-line bg-white/50 transition hover:border-forest">
      <div className={`relative flex aspect-[16/9] items-end p-5 ${tones[guide.tone]}`}>
        <span className="absolute right-5 top-5 opacity-80 transition duration-500 group-hover:-rotate-6 group-hover:scale-110"><GuideIcon icon={guide.icon} size={44} /></span>
        <span className="rounded-full bg-bone/80 px-3 py-1 text-[0.6875rem] font-semibold text-forest-deep">{guide.minutes} min read</span>
      </div>
      <div className="flex flex-1 flex-col p-5">
        <h3 className="font-display text-2xl leading-tight">{guide.title}</h3>
        <p className="mt-2 flex-1 text-sm leading-relaxed text-muted">{guide.summary}</p>
        <span className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-forest">Read the guide <ArrowUpRight size={15} className="transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5" /></span>
      </div>
    </Link>
  );
}
