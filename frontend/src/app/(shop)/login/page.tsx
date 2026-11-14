import type { Metadata } from "next";
import { AuthForm } from "@/components/account/AuthForm";
import { first, type SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Sign in", robots: { index: false } };

export default async function LoginPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  return <AuthForm mode="login" next={first((await searchParams).next)} />;
}
