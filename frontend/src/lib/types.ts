import type { CareLevel, Light, Placement, Water } from "./care";

export type Swatch = { name: string; kind: "GROW_BAG" | "CLAY" | "CERAMIC"; hex: string };

export type ProductCard = {
  id: number;
  slug: string;
  name: string;
  localName: string | null;
  category: string;
  fromPrice: number;
  compareAtPrice: number | null;
  priceVaries: boolean;
  image: string | null;
  hoverImage: string | null;
  planters: Swatch[];
  light: Light;
  careLevel: CareLevel;
  petFriendly: boolean;
  fruiting: string | null;
  isNew: boolean;
  soldOut: boolean;
};

export type CategoryNode = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  image: string | null;
  productCount: number;
  children: CategoryNode[];
};

export type FacetValue = { value: string; label: string; count: number; selected: boolean };

export type ProductPage = {
  items: ProductCard[];
  page: number;
  pageSize: number;
  total: number;
  facets: {
    light: FacetValue[];
    care: FacetValue[];
    placement: FacetValue[];
    sizes: FacetValue[];
    petFriendly: number;
    price: { min: number; max: number };
  };
};

export type Option = {
  variantId: number;
  size: "SMALL" | "MEDIUM" | "LARGE";
  sizeLabel: string;
  height: string;
  sku: string;
  price: number;
  compareAtPrice: number | null;
  stock: number;
  dhakaOnly: boolean;
};

export type PlanterOption = {
  id: number;
  name: string;
  kind: Swatch["kind"];
  hex: string;
  images: { url: string; alt: string }[];
  options: Option[];
};

export type ProductDetail = {
  id: number;
  slug: string;
  name: string;
  localName: string | null;
  botanicalName: string | null;
  breadcrumb: { name: string; slug: string }[];
  description: string;
  details: string[];
  care: {
    light: Light;
    water: Water;
    careLevel: CareLevel;
    placement: Placement;
    petFriendly: boolean;
    matureHeight: string | null;
    growthRate: "SLOW" | "MEDIUM" | "FAST" | null;
    fruiting: string | null;
  };
  isNew: boolean;
  planters: PlanterOption[];
};

export type Collection = {
  id: number;
  name: string;
  slug: string;
  tagline: string | null;
  description: string | null;
  image: string | null;
  productCount: number;
};

export type Home = {
  collections: Collection[];
  categories: CategoryNode[];
  bestSellers: ProductCard[];
  fruitTrees: ProductCard[];
  newArrivals: ProductCard[];
};

export type CartLine = {
  variantId: number;
  productId: number;
  slug: string;
  name: string;
  planter: string;
  planterHex: string;
  size: string;
  height: string;
  sku: string;
  image: string | null;
  unitPrice: number;
  compareAtPrice: number | null;
  quantity: number;
  stock: number;
  lineTotal: number;
  dhakaOnly: boolean;
  problem: "UNAVAILABLE" | "OUT_OF_STOCK" | "NOT_ENOUGH_STOCK" | null;
};

export type Cart = {
  items: CartLine[];
  itemCount: number;
  subtotal: number;
  discount: number;
  coupon: { code: string; description: string; discount: number; freeShipping: boolean } | null;
  couponProblem: string | null;
  total: number;
  freeDeliveryOver: number;
  remainingForFreeDelivery: number;
  dhakaOnly: boolean;
  canCheckout: boolean;
};

export type Zone = "DHAKA" | "OUTSIDE";

export type Quote = {
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  couponCode: string | null;
  problem: string | null;
};

export type OrderView = {
  number: string;
  status: "PENDING_PAYMENT" | "PLACED" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELLED";
  paymentMethod: "COD" | "ONLINE";
  paymentStatus: "UNPAID" | "PAID" | "REFUNDED";
  placedAt: string;
  name: string;
  phone: string;
  email: string | null;
  address: string;
  area: string;
  city: string;
  zone: Zone;
  note: string | null;
  items: { slug: string; name: string; planter: string; size: string; height: string; image: string | null; unitPrice: number; quantity: number; lineTotal: number }[];
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  couponCode: string | null;
  events: { status: string; note: string | null; at: string }[];
};

export type Profile = { id: number; name: string; phone: string; email: string | null };

export type SavedAddress = {
  id: number;
  label: string;
  name: string;
  phone: string;
  address: string;
  area: string;
  city: string;
  zone: Zone;
  note: string | null;
  isDefault: boolean;
};

export type OrderSummary = { number: string; status: OrderView["status"]; placedAt: string; itemCount: number; lineCount: number; total: number; images: string[] };
export type OrderHistory = { items: OrderSummary[]; page: number; pageSize: number; total: number };
export type Reordered = { added: number; unavailable: string[]; cart: Cart };
