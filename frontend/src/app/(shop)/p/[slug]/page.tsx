import type { Metadata } from "next";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { ProductView } from "@/components/product/ProductView";
import { ProductRail } from "@/components/shop/ProductRail";
import { SectionHeading } from "@/components/shop/SectionHeading";
import { site } from "@/config/site";
import { JsonLd, absolute, breadcrumbs } from "@/lib/jsonld";
import { Reviews } from "@/components/review/Reviews";

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<SearchParams> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await api.product((await params).slug);
  const image = product.planters[0]?.images[0]?.url;
  return {
    title: product.localName ? `${product.name} (${product.localName})` : product.name,
    description: product.description.slice(0, 160),
    alternates: { canonical: `/p/${product.slug}` },
    openGraph: { title: product.name, description: product.description, images: image ? [image] : [] },
  };
}

export default async function ProductPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const [product, related, reviews] = await Promise.all([api.product(slug), api.related(slug), api.reviews(slug)]);
  const options = product.planters.flatMap((p) => p.options);
  const prices = options.map((o) => o.price);
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    alternateName: [product.localName, product.botanicalName].filter(Boolean),
    description: product.description,
    image: product.planters.flatMap((p) => p.images.map((i) => absolute(i.url))).slice(0, 4),
    url: absolute(`/p/${product.slug}`),
    brand: { "@type": "Brand", name: site.name },
    offers: {
      "@type": "AggregateOffer",
      priceCurrency: "BDT",
      lowPrice: Math.min(...prices),
      highPrice: Math.max(...prices),
      availability: options.some((o) => o.stock > 0) ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
      offerCount: options.length,
    },
    ...(product.rating !== null && product.reviewCount > 0 && {
      aggregateRating: { "@type": "AggregateRating", ratingValue: product.rating, reviewCount: product.reviewCount, bestRating: 5, worstRating: 1 },
      review: reviews.items.slice(0, 5).map((r) => ({
        "@type": "Review",
        author: { "@type": "Person", name: r.author },
        datePublished: r.createdAt.slice(0, 10),
        reviewBody: r.body,
        ...(r.title && { name: r.title }),
        reviewRating: { "@type": "Rating", ratingValue: r.rating, bestRating: 5, worstRating: 1 },
      })),
    }),
  };
  return (
    <>
      <div className="container-page pt-6 md:pt-10">
        <ProductView product={product} initialPlanter={first((await searchParams).planter)} />
      </div>
      <div className="container-page pt-20 md:pt-28">
        <Reviews slug={product.slug} product={product.name} initial={reviews} />
      </div>
      {related.length > 0 && (
        <section className="container-page pt-20 md:pt-28">
          <SectionHeading kicker="Grows well with" title={<>You may also <em>like</em></>} />
          <ProductRail products={related} />
        </section>
      )}
      <JsonLd data={jsonLd} />
      <JsonLd data={breadcrumbs([{ name: "Home", path: "/" }, ...product.breadcrumb.map((c) => ({ name: c.name, path: `/c/${c.slug}` })), { name: product.name, path: `/p/${product.slug}` }])} />
    </>
  );
}
