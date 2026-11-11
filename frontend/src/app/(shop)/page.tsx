import Link from "next/link";
import { ArrowRight, ArrowUpRight, Boxes, CloudSun, HeartHandshake, PawPrint, ShieldCheck, Sprout, Sun, Truck } from "lucide-react";
import { api } from "@/lib/api";
import { site } from "@/config/site";
import { taka } from "@/lib/money";
import type { Collection, ProductCard } from "@/lib/types";
import { ProductRail } from "@/components/shop/ProductRail";
import { ProductCardView } from "@/components/shop/ProductCardView";
import { SectionHeading } from "@/components/shop/SectionHeading";

/* eslint-disable @next/next/no-img-element */
export default async function HomePage() {
  const home = await api.home();
  const [monsoon, ...others] = home.collections;
  const star = home.fruitTrees[0];

  return (
    <>
      {monsoon && <Hero collection={monsoon} star={star} />}
      <Finder />

      <section className="container-page pt-20 md:pt-28">
        <SectionHeading kicker="Browse the nursery" title={<>Something for every <em>space</em></>} />
        <div className="grid gap-4 md:grid-cols-3">
          {home.categories.map((category, i) => (
            <Link key={category.id} href={`/c/${category.slug}`} className="group relative block animate-rise overflow-hidden rounded-[2rem] bg-mist" style={{ animationDelay: `${i * 90}ms` }}>
              <div className="aspect-[4/5] md:aspect-[3/4]">
                {category.image && <img src={category.image} alt="" className="h-full w-full object-cover transition duration-[1.2s] ease-out-soft group-hover:scale-105" />}
              </div>
              <div className="absolute inset-x-3 bottom-3 rounded-[1.5rem] bg-bone/90 p-5 backdrop-blur">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-display text-3xl leading-none">{category.name}</p>
                    <p className="mt-2 text-xs text-muted">{category.productCount} plants · {category.description}</p>
                  </div>
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-forest text-bone transition group-hover:bg-lime group-hover:text-forest-deep">
                    <ArrowUpRight size={18} />
                  </span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <section className="container-page pt-20 md:pt-28">
        <SectionHeading kicker="Most loved" title="Best sellers" href="/shop?sort=popular" />
        <ProductRail products={home.bestSellers} />
      </section>

      <FruitFeature trees={home.fruitTrees} />

      <section className="container-page pt-20 md:pt-28">
        <SectionHeading kicker="Collections" title={<>Picked for the <em>season</em></>} />
        <div className="grid gap-4 md:grid-cols-2">
          {others.map((c) => <CollectionCard key={c.id} collection={c} />)}
        </div>
      </section>

      <HowItArrives />
    </>
  );
}

function Hero({ collection, star }: { collection: Collection; star?: ProductCard }) {
  return (
    <section className="px-2 pt-2 md:px-3 md:pt-3">
      <div className="relative grid overflow-hidden rounded-[2rem] bg-sage md:rounded-[2.5rem] lg:min-h-[min(calc(100dvh-8rem),48rem)] lg:grid-cols-2">
        <div className="relative z-10 flex flex-col justify-center px-6 py-12 md:px-12 lg:py-20 xl:px-20">
          <p className="inline-flex w-fit animate-rise items-center gap-2 rounded-full bg-bone/70 px-3 py-1.5 text-xs font-semibold">
            <span className="h-2 w-2 rounded-full bg-moss" /> {collection.name} is here
          </p>
          <h1 className="mt-6 animate-rise font-display text-[3.25rem] leading-[0.95] tracking-tight [animation-delay:80ms] sm:text-7xl xl:text-[5.5rem]">
            Plant a tree.<br /><em className="text-forest">Grow a legacy.</em>
          </h1>
          <p className="mt-6 max-w-md animate-rise text-base leading-relaxed text-ink-soft [animation-delay:160ms]">
            Grafted fruit trees, flowering giants and easy indoor plants — raised in our nursery, hardened in real sun
            and delivered potted, ready to grow.
          </p>
          <div className="mt-9 flex animate-rise flex-wrap gap-3 [animation-delay:240ms]">
            <Link href={`/collections/${collection.slug}`} className="group inline-flex items-center gap-2 rounded-full bg-forest px-7 py-4 text-sm font-semibold text-bone transition hover:bg-forest-deep">
              Shop {collection.name.toLowerCase()} <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
            </Link>
            <Link href="/c/indoor" className="inline-flex items-center rounded-full bg-bone/70 px-7 py-4 text-sm font-semibold transition hover:bg-bone">
              Indoor plants
            </Link>
          </div>
          <ul className="mt-12 flex flex-wrap gap-x-6 gap-y-2 text-xs font-semibold text-ink-soft">
            <li className="flex items-center gap-1.5"><Sprout size={14} /> Grafted &amp; hardened</li>
            <li className="flex items-center gap-1.5"><ShieldCheck size={14} /> {site.guaranteeDays}-day guarantee</li>
            <li className="flex items-center gap-1.5"><Truck size={14} /> Delivered potted</li>
          </ul>
        </div>
        <div className="relative min-h-[22rem] lg:min-h-0">
          {collection.image && <img src={collection.image} alt="" className="absolute inset-0 h-full w-full object-cover" />}
          {star && (
            <Link href={`/p/${star.slug}`} className="absolute bottom-4 left-4 right-4 flex animate-rise items-center gap-4 rounded-[1.5rem] bg-bone/90 p-3 pr-5 shadow-xl backdrop-blur [animation-delay:400ms] sm:right-auto sm:max-w-sm md:bottom-8 md:left-8">
              {star.image && <img src={star.image} alt="" className="h-20 w-16 shrink-0 rounded-2xl object-cover" />}
              <div className="min-w-0">
                <p className="kicker text-moss">Plant of the month</p>
                <p className="font-display text-2xl leading-tight">{star.name}</p>
                <p className="text-xs text-muted">{star.fruiting} · from {taka(star.fromPrice)}</p>
              </div>
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}

function Finder() {
  const needs = [
    { icon: Sun, title: "A sunny rooftop", text: "Fruit and flowers that love heat", href: "/shop?light=full_sun&placement=outdoor" },
    { icon: CloudSun, title: "A dim corner", text: "Plants that cope with low light", href: "/shop?light=low,medium&placement=indoor" },
    { icon: PawPrint, title: "Curious pets", text: "Safe around cats and dogs", href: "/shop?petFriendly=true" },
    { icon: Sprout, title: "A first plant", text: "Forgiving, easy-care picks", href: "/shop?care=easy&sort=popular" },
  ];
  return (
    <section className="container-page pt-14 md:pt-20">
      <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
        <h2 className="font-display text-3xl md:text-4xl">Find a plant for…</h2>
        <p className="max-w-sm text-sm text-muted">Tell us about your space and we&rsquo;ll show the plants that will thrive there.</p>
      </div>
      <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {needs.map(({ icon: Icon, title, text, href }) => (
          <Link key={title} href={href} className="group flex flex-col justify-between gap-8 rounded-[1.75rem] border border-line bg-white/50 p-5 transition hover:border-forest hover:bg-forest hover:text-bone md:p-6">
            <span className="grid h-11 w-11 place-items-center rounded-full bg-mist text-forest transition group-hover:bg-lime group-hover:text-forest-deep">
              <Icon size={20} strokeWidth={1.7} />
            </span>
            <span>
              <span className="block font-display text-2xl leading-tight">{title}</span>
              <span className="mt-1 block text-xs text-muted transition group-hover:text-bone/70">{text}</span>
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}

function FruitFeature({ trees }: { trees: ProductCard[] }) {
  return (
    <section className="px-2 pt-20 md:px-3 md:pt-28">
      <div className="rounded-[2rem] bg-forest-deep py-16 text-bone md:rounded-[2.5rem] md:py-24">
        <div className="container-page grid gap-12 lg:grid-cols-12">
          <div className="lg:col-span-4">
            <p className="kicker text-lime">Fruit trees</p>
            <h2 className="mt-4 font-display text-5xl leading-[1] md:text-6xl">Fruit in your first few <em className="text-lime">summers.</em></h2>
            <p className="mt-6 text-sm leading-relaxed text-bone/70">
              Seed-grown trees can take ten years to fruit. Ours are grafted or air-layered from proven mother trees, so most
              bear in one to three years — and the fruit tastes exactly like its parent.
            </p>
            <ul className="mt-8 space-y-3 text-sm">
              {["Grafted from heavy-bearing mother trees", "Hardened in full sun before dispatch", "Care card and WhatsApp plant doctor"].map((t) => (
                <li key={t} className="flex items-center gap-3"><span className="grid h-6 w-6 place-items-center rounded-full bg-lime text-forest-deep"><Sprout size={13} /></span>{t}</li>
              ))}
            </ul>
            <Link href="/c/fruit-trees" className="mt-10 inline-flex items-center gap-2 rounded-full bg-lime px-7 py-4 text-sm font-semibold text-forest-deep">
              All fruit trees <ArrowRight size={16} />
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-x-4 gap-y-10 self-center sm:grid-cols-3 lg:col-span-8 [&_h3]:text-bone [&_p]:text-bone/60 [&_p_.text-ink]:text-bone">
            {trees.slice(0, 3).map((t) => <ProductCardView key={t.id} product={t} />)}
          </div>
        </div>
      </div>
    </section>
  );
}

function CollectionCard({ collection }: { collection: Collection }) {
  return (
    <Link href={`/collections/${collection.slug}`} className="group relative block overflow-hidden rounded-[2rem] bg-mist">
      <div className="aspect-[4/3]">
        {collection.image && <img src={collection.image} alt="" loading="lazy" className="h-full w-full object-cover transition duration-[1.2s] ease-out-soft group-hover:scale-105" />}
      </div>
      <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 bg-gradient-to-t from-forest-deep/80 via-forest-deep/30 to-transparent p-6 pt-28 text-bone md:p-8">
        <div>
          <p className="kicker text-lime">{collection.productCount} plants</p>
          <p className="mt-2 font-display text-4xl leading-none">{collection.name}</p>
          <p className="mt-2 text-sm text-bone/80">{collection.tagline}</p>
        </div>
        <span className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-bone text-forest-deep transition group-hover:bg-lime"><ArrowUpRight size={20} /></span>
      </div>
    </Link>
  );
}

function HowItArrives() {
  const steps = [
    { icon: Sprout, title: "Raised in our nursery", text: "Every plant is grown and hardened outdoors, so it is ready for real conditions." },
    { icon: Boxes, title: "Packed to travel", text: "Soil wrapped, stems supported and boxes ventilated. Large trees go by our own van." },
    { icon: Truck, title: "At your door in 1–4 days", text: `Inside Dhaka ${taka(site.deliveryFees.insideDhaka)}, outside Dhaka ${taka(site.deliveryFees.outsideDhaka)}. Free over ${taka(site.freeDeliveryOver)}.` },
    { icon: HeartHandshake, title: `${site.guaranteeDays}-day guarantee`, text: "If it arrives unhealthy or struggles in the first month, we replace it." },
  ];
  return (
    <section className="container-page pt-20 md:pt-28">
      <SectionHeading kicker="From our soil to yours" title={<>How your plant <em>arrives</em></>} />
      <ol className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        {steps.map(({ icon: Icon, title, text }, i) => (
          <li key={title} className="relative rounded-[1.75rem] bg-mist p-6 md:p-7">
            <span className="absolute right-6 top-5 font-display text-5xl text-forest/15">0{i + 1}</span>
            <Icon size={24} strokeWidth={1.5} className="text-moss" />
            <p className="mt-8 font-display text-2xl">{title}</p>
            <p className="mt-2 text-sm leading-relaxed text-muted">{text}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
