import type { MetadataRoute } from "next";
import { site } from "@/config/site";

/** Search engines may read the shop, not the private or per-person pages. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/admin", "/account", "/checkout", "/cart", "/order/", "/track", "/search", "/login", "/register", "/forgot-password", "/api/"],
    },
    sitemap: `${site.url}/sitemap.xml`,
    host: site.url,
  };
}
