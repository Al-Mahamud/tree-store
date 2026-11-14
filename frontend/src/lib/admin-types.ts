import type { OrderView, Zone } from "./types";
import type { CareLevel, Light, Placement, Water } from "./care";

export type Staff = { id: number; name: string; phone: string };
export type OrderStatus = OrderView["status"];
export type PaymentMethod = OrderView["paymentMethod"];
export type PaymentStatus = OrderView["paymentStatus"];

export type AdminRow = {
  number: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  placedAt: string;
  name: string;
  phone: string;
  area: string;
  city: string;
  zone: Zone;
  /** Has a plant that can only go inside Dhaka. */
  dhakaOnly: boolean;
  itemCount: number;
  total: number;
};

export type AdminOrderPage = { items: AdminRow[]; page: number; pageSize: number; total: number; counts: Record<string, number> };

export type AdminAttempt = { tranId: string; amount: number; status: string; cardType: string | null; bankTranId: string | null; at: string };

export type AdminOrderDetail = {
  order: OrderView;
  next: OrderStatus[];
  attempts: AdminAttempt[];
  refundedAt: string | null;
  refundReference: string | null;
  refundDue: boolean;
  customerId: number | null;
  customerOrders: number;
  /** SKUs of the items, in the same order. */
  skus: string[];
};

export type Dashboard = {
  ordersToday: number;
  revenueToday: number;
  averageOrder: number;
  open: Record<string, number>;
  refundsDue: number;
  lowStock: number;
  awaitingPayment: number;
  week: { date: string; orders: number; revenue: number }[];
  topPlants: { name: string; slug: string; image: string | null; quantity: number; revenue: number }[];
  recent: AdminRow[];
};

export type ProductStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type Size = "SMALL" | "MEDIUM" | "LARGE";
export type PlanterKind = "GROW_BAG" | "CLAY" | "CERAMIC";
export type Growth = "SLOW" | "MEDIUM" | "FAST";

export type AdminPlanter = { id: number; name: string; kind: PlanterKind; hex: string };
export type AdminVariant = { id: number; planterId: number; size: Size; height: string; sku: string; price: number; compareAtPrice: number | null; stock: number; dhakaOnly: boolean };
export type AdminImage = { id: number; planterId: number | null; url: string; alt: string };

export type AdminProduct = {
  id: number;
  name: string;
  localName: string | null;
  botanicalName: string | null;
  slug: string;
  categoryId: number;
  category: string;
  description: string;
  details: string;
  light: Light;
  water: Water;
  careLevel: CareLevel;
  placement: Placement;
  petFriendly: boolean;
  matureHeight: string | null;
  growthRate: Growth | null;
  fruiting: string | null;
  featured: boolean;
  status: ProductStatus;
  soldCount: number;
  collectionIds: number[];
  planters: AdminPlanter[];
  variants: AdminVariant[];
  images: AdminImage[];
};

export type AdminProductRow = {
  id: number;
  name: string;
  localName: string | null;
  botanicalName: string | null;
  slug: string;
  image: string | null;
  category: string;
  status: ProductStatus;
  featured: boolean;
  optionCount: number;
  fromPrice: number;
  stock: number;
  low: boolean;
  soldCount: number;
};
export type AdminProductPage = { items: AdminProductRow[]; page: number; pageSize: number; total: number; counts: Record<ProductStatus, number> };

export type StockRow = { variantId: number; productId: number; product: string; image: string | null; planter: string; size: Size; height: string; sku: string; price: number; stock: number; dhakaOnly: boolean; status: ProductStatus };

export type Coupon = { id: number; code: string; description: string; kind: "PERCENT" | "FIXED" | "FREE_SHIPPING"; value: number; maxDiscount: number | null; minSubtotal: number; startsAt: string | null; endsAt: string | null; usageLimit: number | null; usedCount: number; active: boolean };

export type DeliveryFees = { insideDhaka: number; outsideDhaka: number; freeOver: number; updatedAt: string | null; updatedBy: string | null };

export type Text = { id: number; phone: string; kind: "CODE" | "ORDER_PLACED" | "ORDER_SHIPPED" | "ORDER_CANCELLED" | "CARE_REMINDER"; body: string; status: "QUEUED" | "SENDING" | "SENT" | "FAILED"; attempts: number; provider: string | null; error: string | null; orderNumber: string | null; createdAt: string; sentAt: string | null };
export type TextPage = { items: Text[]; page: number; pageSize: number; total: number; counts: Record<string, number> };

export type StaffRow = { id: number; name: string; phone: string; active: boolean; createdAt: string; lastSignedIn: string | null };

export type CategoryAdmin = { id: number; parentId: number | null; name: string; slug: string; description: string | null; image: string | null; position: number; products: number; children: CategoryAdmin[] };

export type CollectionAdmin = { id: number; name: string; slug: string; tagline: string | null; description: string | null; image: string | null; active: boolean; position: number; products: number };
