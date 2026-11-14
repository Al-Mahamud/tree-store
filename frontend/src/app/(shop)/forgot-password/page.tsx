import type { Metadata } from "next";
import { ResetForm } from "@/components/account/ResetForm";

export const metadata: Metadata = { title: "Reset your password", robots: { index: false } };

export default function ForgotPasswordPage() {
  return <ResetForm />;
}
