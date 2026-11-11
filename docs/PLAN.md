# Shobuj — online tree and plant shop: frontend and backend plan

Shobuj (সবুজ, "green") is a working name for an online nursery in Bangladesh selling fruit trees,
flowering trees, shade and timber saplings, palms, bonsai and indoor plants, delivered potted and
ready to plant. The name, colours and contact details each live in one file.

## 1. Goals

- A storefront that feels like a modern plant brand: calm, botanical, photo-led, quick on phones.
- Help people choose: every plant shows light, water, care level, mature height, pet safety and,
  for fruit trees, how soon it bears fruit. Shoppers can filter by all of these.
- Buy as a guest or with an account, pay cash on delivery or online, and track the order.
- The owner manages plants, stock, prices, orders, coupons and delivery areas in an admin area.

Out of scope for v1: marketplace sellers, subscriptions, gardener booking calendar, mobile app.

## 2. What makes a plant shop different from other shops

| Topic | Decision |
|---|---|
| Options | Each plant comes in **sizes** (Small 1–2 ft, Medium 3–4 ft, Large 5–6 ft) and **planters** (grow bag, clay pot, ceramic pot). Each size × planter pair is one stock item with its own price |
| Price | Price belongs to the size × planter pair; product cards show "from ৳…" |
| Images | Images belong to a planter, so choosing "Ceramic pot" shows the plant in that pot |
| Care facts | Light (low, medium, bright, full sun), water (low, medium, high), care level (easy, moderate, expert), indoor/outdoor, pet-friendly, mature height, growth rate, fruiting time |
| Names | English name, Bangla name (আম) and botanical name (*Mangifera indica*), all searchable |
| Delivery | Live plants: inside Dhaka ৳100, outside Dhaka ৳200, free over ৳5,000. Large trees are **Dhaka only** (marked per size); checkout refuses them for outside Dhaka with a clear message |
| Guarantee | 30-day healthy-plant guarantee (shown in copy; claims handled by support in v1) |
| Extras (later) | Planting service add-on (Dhaka), gift message, care reminders by SMS |

## 3. Architecture

```text
Browser ──► Next.js (frontend, :3000) ──► Spring Boot API (backend, :8080) ──► PostgreSQL
            server-renders pages            /api/v1/** JSON                     Flyway migrations
            forwards /api and /media        session cookie + CSRF               images on disk
```

Java 17, Spring Boot 4.1, Spring JDBC, PostgreSQL 18, embedded PostgreSQL for tests and local runs
(no Docker). Next.js 16, React 19, Tailwind CSS 4, TypeScript.

## 4. Backend plan

### 4.1 Packages

| Package | Owns |
|---|---|
| `catalog` | categories, collections, plants, planters, sizes, images, care facts, search and filters |
| `checkout` | bag (cookie), coupons, delivery zones and Dhaka-only rules, prices, placing orders |
| `order` | orders, items, status history, tracking |
| `customer` | accounts, sign-in, addresses, wishlist (phase 4) |
| `payment` | SSLCommerz and a fake local gateway (phase 5) |
| `admin` | admin endpoints, dashboard numbers (phase 6) |
| `review`, `notify` | reviews with photos, order SMS/email, care reminders (phase 7) |
| `shared` | errors, security, settings, phone numbers, clock |

### 4.2 Data model

```text
categories(id, parent_id, name, slug, description, image_url, position)
collections(id, name, slug, tagline, description, image_url, position, active)
products(id, name, local_name, botanical_name, slug, category_id, description, details,
         light, water, care_level, placement[INDOOR|OUTDOOR|BOTH], pet_friendly,
         mature_height, growth_rate, fruiting, status, featured, sold_count, created_at, search)
product_collections(product_id, collection_id)
planters(id, product_id, name, kind[GROW_BAG|CLAY|CERAMIC], hex, position)
product_images(id, product_id, planter_id?, url, alt, position)
variants(id, product_id, planter_id, size, height, position, sku, price, compare_at_price,
         stock, dhaka_only)
carts, cart_items, coupons                       (same rules as any shop)
orders(..., zone, subtotal, discount, shipping_fee, total, payment_method, status ...)
order_items(..., product_name, planter, size, height, unit_price, quantity)
order_events(order_id, status, note, actor, created_at)
```

### 4.3 Order statuses

```text
PENDING_PAYMENT ─paid─► PLACED ─► CONFIRMED ─► SHIPPED ─► DELIVERED
        └─expired─► CANCELLED ◄── (PLACED or CONFIRMED) — stock goes back
Cash-on-delivery orders start at PLACED.
```

### 4.4 API (under `/api/v1`)

- `GET /home`, `GET /categories`, `GET /collections[/{slug}]`
- `GET /products?category=&collection=&q=&light=&care=&placement=&petFriendly=&size=&minPrice=&maxPrice=&sale=&sort=&page=`
  — cards plus facet counts
- `GET /products/{slug}`, `GET /products/{slug}/related`
- `GET/POST/PATCH/DELETE /cart…`, `POST/DELETE /cart/coupon`
- `POST /checkout/quote` (zone → totals and any Dhaka-only problem), `POST /checkout`
- `GET /orders/{number}?token=`, `GET /orders/track?number=&phone=`
- Phases 4–7: `/auth/*`, `/account/*`, `/payments/*`, `/admin/*`, reviews

## 5. Frontend plan

### 5.1 Look and feel

- Botanical and calm: bone-white background, deep forest green text, sage panels and a fresh
  lime highlight used sparingly.
- Soft, organic shapes: rounded image cards, pill buttons and chips (a clear contrast to sharp,
  editorial fashion layouts).
- Type: Instrument Serif for large headings, Manrope for everything else.
- Care facts as small icon tiles (sun, droplet, leaf, paw, ruler), so a plant can be judged at a glance.
- Mobile first; accessible labels and focus states; reduced-motion respected.

### 5.2 Pages

| Route | Content |
|---|---|
| `/` | hero, "find your plant" by light and space, category tiles, best sellers, fruit-tree feature, guarantee strip, collections |
| `/shop`, `/c/[slug]`, `/collections/[slug]`, `/search` | grid with filters: light, care level, indoor/outdoor, pet-friendly, size, price, offers |
| `/p/[slug]` | gallery, planter picker, size cards with height and price, care facts, delivery notes, description, related |
| bag drawer, `/cart` | lines with size and planter, coupon, free-delivery progress |
| `/checkout` | contact, address, zone (with Dhaka-only warning), payment, summary |
| `/order/[number]`, `/track` | thank-you with timeline; guest tracking |
| `/account/*`, `/login`, `/register` | phase 4 |
| `/admin/*` | phase 6 |
| `/guides/[slug]`, `/pages/[slug]` | planting guides, delivery, guarantee, contact (phase 7) |

## 6. Delivery phases

| # | Phase | Contents | Status |
|---|---|---|---|
| 0 | Plan and skeleton | this plan, both projects, README | |
| 1 | Catalog backend | schema, filters with facets, product pages, home, demo nursery, tests | |
| 2 | Storefront | design system, header/footer, home, listings, product page | |
| 3 | Bag and checkout | bag, coupons, zones with Dhaka-only rule, COD orders, thank-you, tracking | |
| 4 | Accounts | register/sign in, bag merge, addresses, order history, wishlist | |
| 5 | Online payment | SSLCommerz with a fake local gateway, payment expiry | |
| 6 | Admin | dashboard, plant editor with images, stock grid, orders, coupons, delivery settings | |
| 7 | Content and care | reviews, order SMS/email, care-reminder SMS, planting guides, SEO, sitemap | |
| 8 | Launch readiness | Docker, Caddy, backups, CI, real payment credentials | |
