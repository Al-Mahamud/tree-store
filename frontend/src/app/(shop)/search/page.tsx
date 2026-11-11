import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import { first, type SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Search", robots: { index: false } };

export default async function SearchPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = await searchParams;
  const q = first(params.q)?.trim() ?? "";
  return (
    <Listing
      scope={{}}
      searchParams={params}
      kicker="Search results"
      title={q ? <>&ldquo;<em>{q}</em>&rdquo;</> : "Search"}
      crumbs={[{ name: "Search", href: `/search?q=${encodeURIComponent(q)}` }]}
    />
  );
}
