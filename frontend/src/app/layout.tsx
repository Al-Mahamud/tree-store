import type { Metadata, Viewport } from "next";
import { Instrument_Serif, Manrope, Noto_Sans_Bengali } from "next/font/google";
import { site } from "@/config/site";
import "./globals.css";

const manrope = Manrope({ subsets: ["latin"], variable: "--font-manrope", display: "swap" });
const instrument = Instrument_Serif({
  subsets: ["latin"],
  weight: "400",
  style: ["normal", "italic"],
  variable: "--font-instrument",
  display: "swap",
});
// Bangla plant names and the taka sign (৳).
const bengali = Noto_Sans_Bengali({ subsets: ["bengali"], variable: "--font-bengali", display: "swap", weight: ["400", "500", "600"] });

export const metadata: Metadata = {
  title: { default: `${site.name} — ${site.tagline}`, template: `%s · ${site.name}` },
  description: site.description,
};

export const viewport: Viewport = { themeColor: "#f4f2ec" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${manrope.variable} ${instrument.variable} ${bengali.variable}`}>
      <body className="min-h-dvh font-sans">{children}</body>
    </html>
  );
}
