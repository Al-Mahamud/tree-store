import { connection } from "next/server";
import { Header } from "@/components/shop/Header";
import { Footer } from "@/components/shop/Footer";
import { AccountProvider } from "@/components/account/AccountProvider";
import { WishlistProvider } from "@/components/account/WishlistProvider";
import { CartProvider } from "@/components/cart/CartProvider";
import { CartDrawer } from "@/components/cart/CartDrawer";

export default async function ShopLayout({ children }: { children: React.ReactNode }) {
  // Pages render when visited, not at build time: the build (CI, Docker) has no shop API to ask.
  // Catalog reads are still cached for their revalidate time (lib/api.ts).
  await connection();
  return (
    <AccountProvider>
      <WishlistProvider>
        <CartProvider>
          <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-full focus:bg-forest focus:px-4 focus:py-2 focus:text-bone">
            Skip to content
          </a>
          <Header />
          <main id="main">{children}</main>
          <Footer />
          <CartDrawer />
        </CartProvider>
      </WishlistProvider>
    </AccountProvider>
  );
}
