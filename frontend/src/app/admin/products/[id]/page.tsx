"use client";

import { use } from "react";
import { ProductEditor } from "@/components/admin/ProductEditor";

export default function EditPlantPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <ProductEditor key={id} id={Number(id)} />;
}
