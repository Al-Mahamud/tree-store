import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import type { SearchParams } from "@/lib/listing";
import { api } from "@/lib/api";

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<SearchParams> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const collection = await api.collection((await params).slug);
  return {
    title: collection.name,
    description: collection.description ?? undefined,
    alternates: { canonical: `/collections/${collection.slug}` },
    openGraph: collection.image ? { images: [collection.image] } : undefined,
  };
}

export default async function CollectionPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const [collection, all] = await Promise.all([api.collection(slug), api.collections()]);
  return (
    <Listing
      scope={{ collection: slug }}
      searchParams={await searchParams}
      kicker={collection.tagline ?? "Collection"}
      title={collection.name}
      description={collection.description}
      image={collection.image}
      crumbs={[{ name: collection.name, href: `/collections/${slug}` }]}
      tabs={all.map((c) => ({ name: c.name, href: `/collections/${c.slug}`, active: c.slug === slug }))}
    />
  );
}
