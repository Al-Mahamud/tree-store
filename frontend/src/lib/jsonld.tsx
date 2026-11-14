import { site } from "@/config/site";

/** Structured data for search engines, as a <script> (with "<" escaped so text can't close the tag). */
export function JsonLd({ data }: { data: object }) {
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }} />;
}

/** Absolute address for a path or a /media picture. */
export function absolute(path: string): string {
  return path.startsWith("http") ? path : `${site.url}${path.startsWith("/") ? "" : "/"}${path}`;
}

export function breadcrumbs(items: { name: string; path: string }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, i) => ({ "@type": "ListItem", position: i + 1, name: item.name, item: absolute(item.path) })),
  };
}
