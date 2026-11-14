import Link from "next/link";
import { ChevronDown } from "lucide-react";
import { AccountButton } from "@/components/account/AccountButton";
import { api } from "@/lib/api";
import { site } from "@/config/site";
import { Logo } from "./Logo";
import { MobileMenu } from "./MobileMenu";
import { SearchButton } from "./SearchButton";
import { BagButton } from "./BagButton";

/* eslint-disable @next/next/no-img-element */
export async function Header() {
  const [categories, collections] = await Promise.all([api.categories(), api.collections()]);
  return (
    <>
      <div className="bg-forest-deep text-bone">
        <p className="container-page flex items-center justify-center gap-2 py-2 text-xs">
          <span className="h-1.5 w-1.5 rounded-full bg-lime" aria-hidden />
          Free delivery over ৳{site.freeDeliveryOver.toLocaleString("en-IN")}
          <span className="hidden sm:inline">· {site.guaranteeDays}-day healthy-plant guarantee</span>
        </p>
      </div>
      <header className="sticky top-0 z-40 border-b border-line/70 bg-bone/85 backdrop-blur-md">
        <div className="container-page flex h-16 items-center justify-between gap-4 md:h-[4.5rem]">
          <div className="flex items-center gap-2">
            <MobileMenu categories={categories} collections={collections} />
            <Logo />
          </div>

          <nav aria-label="Main" className="hidden lg:block">
            <ul className="flex items-center gap-1 rounded-full border border-line bg-white/50 p-1">
              {categories.map((top) => (
                <li key={top.id} className="group relative">
                  <Link href={`/c/${top.slug}`} className="inline-flex items-center gap-1 rounded-full px-4 py-2 text-sm font-semibold transition hover:bg-forest hover:text-bone group-focus-within:bg-forest group-focus-within:text-bone">
                    {top.name}
                    {top.children.length > 0 && <ChevronDown size={14} className="transition group-hover:rotate-180" />}
                  </Link>
                  {top.children.length > 0 && (
                    <div className="invisible absolute left-1/2 top-full z-50 w-[40rem] -translate-x-1/2 pt-3 opacity-0 transition duration-200 group-focus-within:visible group-focus-within:opacity-100 group-hover:visible group-hover:opacity-100">
                      <div className="grid grid-cols-5 gap-5 rounded-[2rem] border border-line bg-bone p-5 shadow-[0_30px_60px_-30px_rgba(19,32,26,0.35)]">
                        <ul className="col-span-2 space-y-1 py-2">
                          {top.children.map((child) => (
                            <li key={child.id}>
                              <Link href={`/c/${child.slug}`} className="flex items-baseline justify-between rounded-2xl px-3 py-2.5 hover:bg-mist">
                                <span className="font-display text-xl">{child.name}</span>
                                <span className="text-xs text-muted">{child.productCount}</span>
                              </Link>
                            </li>
                          ))}
                          <li className="px-3 pt-2">
                            <Link href={`/c/${top.slug}`} className="text-sm font-semibold text-moss underline-offset-4 hover:underline">
                              All {top.name.toLowerCase()} →
                            </Link>
                          </li>
                        </ul>
                        <div className="col-span-3 grid grid-cols-2 gap-3">
                          {top.children.slice(0, 2).map((child) => (
                            <Link key={child.id} href={`/c/${child.slug}`} className="group/tile relative block overflow-hidden rounded-3xl bg-mist">
                              <div className="aspect-[4/5]">
                                {child.image && <img src={child.image} alt="" className="h-full w-full object-cover transition duration-700 group-hover/tile:scale-105" />}
                              </div>
                              <span className="absolute bottom-2 left-2 rounded-full bg-bone/90 px-3 py-1 text-xs font-semibold backdrop-blur">{child.name}</span>
                            </Link>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </li>
              ))}
              <li>
                <Link href="/collections/gifts-that-grow" className="inline-flex rounded-full px-4 py-2 text-sm font-semibold transition hover:bg-forest hover:text-bone">
                  Gifts
                </Link>
              </li>
              <li>
                <Link href="/shop?sale=true" className="inline-flex rounded-full px-4 py-2 text-sm font-semibold text-sale transition hover:bg-sale hover:text-white">
                  Offers
                </Link>
              </li>
            </ul>
          </nav>

          <div className="flex items-center gap-1">
            <SearchButton />
            <AccountButton />
            <BagButton />
          </div>
        </div>
      </header>
    </>
  );
}
