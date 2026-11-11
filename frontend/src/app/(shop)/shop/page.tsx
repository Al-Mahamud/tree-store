import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import { first, type SearchParams } from "@/lib/listing";
import { api } from "@/lib/api";

export const metadata: Metadata = { title: "All plants" };

export default async function ShopPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = await searchParams;
  const categories = await api.categories();
  const sale = first(params.sale) === "true";
  const sort = first(params.sort);
  const title = sale ? <>Plants on <em>offer</em></> : sort === "popular" ? <>Best <em>sellers</em></> : sort === "newest" ? <>Just <em>arrived</em></> : <>Every plant, <em>all in one place</em></>;
  return (
    <Listing
      scope={{}}
      searchParams={params}
      kicker={sale ? "Offers" : "The nursery"}
      title={title}
      description={sale ? "Healthy plants at a gentler price while stocks last." : "Filter by light, space, care and budget to find plants that will thrive with you."}
      crumbs={[{ name: "All plants", href: "/shop" }]}
      tabs={[{ name: "All", href: "/shop", active: true }, ...categories.map((c) => ({ name: c.name, href: `/c/${c.slug}`, active: false }))]}
    />
  );
}
