import type { Metadata } from "next";
import { AuthForm } from "@/components/account/AuthForm";
import { first, type SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Create an account", robots: { index: false } };

export default async function RegisterPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  return <AuthForm mode="register" next={first((await searchParams).next)} />;
}
