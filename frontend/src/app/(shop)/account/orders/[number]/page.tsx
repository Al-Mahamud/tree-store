import { AccountOrder } from "@/components/account/AccountOrder";

export default async function AccountOrderPage({ params }: { params: Promise<{ number: string }> }) {
  const { number } = await params;
  return <AccountOrder number={number} />;
}
