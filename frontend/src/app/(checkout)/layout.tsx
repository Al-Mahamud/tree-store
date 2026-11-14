import Link from "next/link";
import { ArrowLeft, Lock } from "lucide-react";
import { AccountProvider } from "@/components/account/AccountProvider";
import { CartProvider } from "@/components/cart/CartProvider";
import { Logo } from "@/components/shop/Logo";

/** Checkout gets a quiet frame: no menus or offers, just the steps to finish the order. */
export default function CheckoutLayout({ children }: { children: React.ReactNode }) {
  return (
    <AccountProvider>
      <CartProvider>
        <header className="container-page grid h-20 grid-cols-[1fr_auto_1fr] items-center">
          <Link href="/cart" className="inline-flex w-fit items-center gap-2 rounded-full bg-mist px-4 py-2 text-sm font-semibold"><ArrowLeft size={15} /><span className="hidden sm:inline">Bag</span></Link>
          <Logo />
          <p className="flex items-center justify-end gap-1.5 text-xs font-semibold text-muted"><Lock size={13} /><span className="hidden sm:inline">Secure checkout</span></p>
        </header>
        <main id="main">{children}</main>
      </CartProvider>
    </AccountProvider>
  );
}
