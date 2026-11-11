import type { Metadata } from "next";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { ProductView } from "@/components/product/ProductView";
import { ProductRail } from "@/components/shop/ProductRail";
import { SectionHeading } from "@/components/shop/SectionHeading";
import { site } from "@/config/site";

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<SearchParams> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await api.product((await params).slug);
  const image = product.planters[0]?.images[0]?.url;
  return {
    title: product.localName ? `${product.name} (${product.localName})` : product.name,
    description: product.description.slice(0, 160),
    openGraph: { title: product.name, description: product.description, images: image ? [image] : [] },
  };
}

export default async function ProductPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const [product, related] = await Promise.all([api.product(slug), api.related(slug)]);
  const options = product.planters.flatMap((p) => p.options);
  const prices = options.map((o) => o.price);
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    alternateName: [product.localName, product.botanicalName].filter(Boolean),
    description: product.description,
    image: product.planters.flatMap((p) => p.images.map((i) => i.url)).slice(0, 4),
    brand: { "@type": "Brand", name: site.name },
    offers: {
      "@type": "AggregateOffer",
      priceCurrency: "BDT",
      lowPrice: Math.min(...prices),
      highPrice: Math.max(...prices),
      availability: options.some((o) => o.stock > 0) ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
    },
  };
  return (
    <>
      <div className="container-page pt-6 md:pt-10">
        <ProductView product={product} initialPlanter={first((await searchParams).planter)} />
      </div>
      {related.length > 0 && (
        <section className="container-page pt-20 md:pt-28">
          <SectionHeading kicker="Grows well with" title={<>You may also <em>like</em></>} />
          <ProductRail products={related} />
        </section>
      )}
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }} />
    </>
  );
}
