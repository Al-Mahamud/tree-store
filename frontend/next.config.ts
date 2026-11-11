import type { NextConfig } from "next";

const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // The browser only talks to Next.js; API calls and images are forwarded to the backend,
  // so cookies stay first-party and there is no CORS.
  async rewrites() {
    return [
      { source: "/api/v1/:path*", destination: `${backend}/api/v1/:path*` },
      { source: "/media/:path*", destination: `${backend}/media/:path*` },
    ];
  },
};

export default nextConfig;
