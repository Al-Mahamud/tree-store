import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import type { SearchParams } from "@/lib/listing";
import { api } from "@/lib/api";

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<SearchParams> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const category = await api.category((await params).slug);
  return {
    title: category.name,
    description: category.description ?? undefined,
    alternates: { canonical: `/c/${category.slug}` },
    openGraph: category.image ? { images: [category.image] } : undefined,
  };
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const [category, tree] = await Promise.all([api.category(slug), api.categories()]);
  const parent = tree.find((top) => top.children.some((c) => c.slug === slug));
  const top = parent ?? category;
  return (
    <Listing
      scope={{ category: slug }}
      searchParams={await searchParams}
      kicker={parent ? parent.name : `${category.productCount} plants`}
      title={category.name}
      description={category.description}
      image={category.image}
      crumbs={parent ? [{ name: parent.name, href: `/c/${parent.slug}` }, { name: category.name, href: `/c/${slug}` }] : [{ name: category.name, href: `/c/${slug}` }]}
      tabs={top.children.length > 0 ? [
        { name: `All ${top.name.toLowerCase()}`, href: `/c/${top.slug}`, active: top.slug === slug },
        ...top.children.map((c) => ({ name: c.name, href: `/c/${c.slug}`, active: c.slug === slug })),
      ] : undefined}
    />
  );
}
