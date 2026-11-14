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
| 0 | Plan and skeleton | this plan, both projects, README | done |
| 1 | Catalog backend | schema, filters with facets, product pages, home, demo nursery, tests | done |
| 2 | Storefront | design system, header/footer, home, listings, product page | done |
| 3 | Bag and checkout | bag, coupons, zones with Dhaka-only rule, COD orders, thank-you, tracking | done |
| 4 | Accounts | register/sign in, bag merge, addresses, order history, wishlist | done |
| 5 | Online payment | SSLCommerz with a fake local gateway, payment expiry | done |
| 6 | Admin | dashboard, plant editor with images, stock grid, orders, coupons, delivery settings | done |
| 7 | Content and care | reviews, order SMS/email, care-reminder SMS, planting guides, SEO, sitemap | done (texts only, no email) |
| 8 | Launch readiness | Docker, Caddy, backups, CI, real payment credentials | |

### Notes from building

- Order numbers look like `SB10001`. Guests open their thank-you page with a random token in the
  link; tracking needs the order number plus the phone number used.
- Search matches English, Bangla and botanical names. Bangla uses a plain "contains" match because
  PostgreSQL's text parser may not split Bangla into words.
- The home page is built at `next build` time and refreshed every 30 s, so the backend must be
  reachable during a production build (handled in phase 8).
- Footer `/pages/*` and `/guides` links lead nowhere until phase 7.

### Notes from phase 4 (accounts)

- Built from the grocery shop's (Haat's) account code, adapted to plants. Customers sign in with a
  mobile number and password (BCrypt; sessions in PostgreSQL; the session id changes at sign-in; 6
  wrong passwords for a number in 15 minutes pause sign-in).
- **Sign-up needs a 6-digit code texted to the number** (brought forward from phase 7, so numbers are
  proven from the start). Codes last 10 minutes, allow 5 tries, one per minute and 5 per hour per
  number. Because the number is proven, earlier guest orders placed with it join the new account.
  "Forgot password" uses the same codes and signs out other devices. Texts go through an outbox in
  the database; `SMS_DRIVER=log` (the default) only logs them, and local runs show them at
  http://localhost:8080/api/v1/test-texts. The SSL Wireless sender is included but unverified.
- One bag per customer, shared across devices; at sign-in the guest bag joins it (larger quantity
  wins, at most 10 per line; a coupon carries over). Signing out leaves the bag with the account.
- Saved addresses (up to 10, one default) include inside/outside Dhaka, so the delivery fee and the
  Dhaka-only rule for large trees work from a saved address too. Checkout picks the default and can
  save a new one.
- Order history with **Order again**: each plant goes back in the bag in the same size and planter,
  raised to the earlier quantity within stock; sold-out options are listed and the bag only opens
  when something is in it.
- **Wishlist**: a heart on every product card and product page. Signed-out shoppers are sent to
  sign in. The header shows how many plants are saved.
- The phone menu and other drawers render into `<body>`; the sticky header's blur made them only as
  tall as the header (the same fix as in Haat).

### Notes from phase 5 (online payment)

- Ported from Haat's tested payment code. An online order waits as PENDING_PAYMENT and holds its
  plants for 30 minutes (`store.payment.expiry`) while the shopper pays on SSLCommerz (bKash, Nagad,
  Rocket, cards).
- A payment counts only when SSLCommerz's Validation API confirms the `val_id` for the same
  `tran_id`, in BDT, for the exact amount; the IPN, the browser's return and a check every minute
  can each report it, and confirming twice changes nothing. Every callback is stored.
- After a failed or cancelled payment the order page offers "Pay now" again (up to 5 tries) or
  "Pay cash on delivery instead". Unpaid orders are cancelled after 30 minutes and the plants go back
  into stock; money that arrives later is recorded for a refund.
- Settings: `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD`, `SSLCOMMERZ_BASE_URL`, `WEB_URL`.
  Online payment is hidden until the credentials are set. Local runs use a fake gateway from the
  test sources ("TEST GATEWAY · NOT REAL MONEY").
- Order texts (confirmed, on the way, cancelled) are left for phase 7.

### Notes from phase 6 (admin)

- Built from Haat's admin, adapted to plants. Staff sign in at `/admin` with a mobile number and
  password (kept apart from customer accounts). Everyone on the team can do everything; switching
  someone off signs them out at once, and nobody can switch off themselves or the last account.
- **Orders**: a "to do" queue (placed, confirmed, on the way), oldest first, filterable by inside or
  outside Dhaka. Placed → Confirmed → Sent out → Delivered. Sending out records the courier and its
  tracking number, which the shopper then sees on their order page. A sent order can still be
  cancelled when the courier brings it back, with a choice to leave the plants out of stock (wilted).
  Cancelling a paid order shows a refund due; the refund is made in the SSLCommerz panel and its
  reference recorded here. Cash orders count as paid when delivered. Print gives a packing slip.
- **Plant editor**: care facts, Bangla and botanical names, planters (name, kind, colour, order) and
  a size × planter grid where each offered option has its own height, price, old price, stock, SKU
  (suggested) and "inside Dhaka only". It all saves at once; options can swap sizes in one save.
  Removing a planter removes its options but keeps its pictures as shared ones. A plant on sale needs
  at least one option, and every planter needs a size.
- **Pictures** save as they are uploaded (JPEG, PNG or WebP, checked by their bytes, up to 5 MB), each
  shown with every planter or with one planter only; the first is the one on cards.
- **Stock**: one small grid per plant (planters × sizes), saved per box. "Running low" means 3 or
  fewer (`store.admin.low-stock`).
- **Categories** (two levels, with a picture and description) and **collections** (banner, tagline,
  order on the home page, hidden or shown). Web addresses are fixed when made.
- **Delivery fees** are set in the admin and stored in the database; the shop, bag and checkout read
  them from `GET /api/v1/delivery`. Until they are saved, the fees in `application.yml` apply.
- Dashboard: today's orders and sales, what to confirm and send, refunds due, running low, the last
  7 days and the best sellers of the last 30 days.
- Also fixed: a delivered cash order said "Paid online" on the shopper's order page.

### Notes from phase 7 (reviews, texts, guides, search engines)

- **Order texts** go through the outbox built in phase 4 (saved with the change, sent every
  5 seconds, retried 3 times). Customers are texted when an order is received (cash at checkout, an
  online order once paid, or switched to cash), when it is sent out (with the courier and tracking
  number, and the cash to keep ready), and when it is cancelled (refund, "not charged" or "call us"
  wording). Confirming (done by phone) and delivery aren't texted. Every text uses "Tk" and the plain
  SMS alphabet so it fits one 160-character SMS; a test checks every care text for every plant.
- **Email** is not sent: texts reach every customer, and email needs a mail provider. Left for later.
- **Care texts**: when an order is marked delivered, three texts are scheduled for 10 am Dhaka time
  on days 2 (settling in: shade first for outdoor trees, light for indoor plants), 14 (watering, from
  the plant's water need, with a link to the guides) and 25 (the 30-day guarantee ends in 5 days).
  Each talks about the dearest plant in the order and is written when sent. A job every 5 minutes
  queues what is due, only between 9 am and 8 pm, and drops reminders more than two days late (the
  app was down). Shoppers can untick them at checkout or switch them off on the order page (guests
  with the order link, or signed in).
- **Reviews**: only a customer with a delivered order holding the plant can review it, once (they can
  change or delete it). 1–5 stars, an optional headline, words (10–2,000 characters) and up to
  3 photos (JPEG, PNG or WebP, up to 5 MB, checked by their bytes). Reviews show at once, with the
  first name and last initial, "Verified buyer", the size and planter bought, and the date. Staff can
  hide a review or reply under it (Admin → Reviews, with tabs for no reply yet, 1–2 stars, photos
  and hidden). Products keep their average and count, shown on cards, the product page (sorted by
  newest, photos, highest or lowest) and in structured data. Account → Reviews lists delivered plants
  waiting for a review. Photo files stay on disk when a photo or review is deleted.
- **Guides** (`frontend/src/config/guides.ts`): six planting and care guides at `/guides`, each with
  plants for it; plant pages link to the guide for their category. **Information pages**
  (`frontend/src/config/pages.ts`): our story, delivery (live fees), guarantee, contact, bulk orders,
  privacy and terms. Both are drafts: the owner must check every promise, and a lawyer should read
  Terms and Privacy before launch.
- **Search engines**: `NEXT_PUBLIC_SITE_URL` sets the public address for canonical links, the
  sitemap (home, shop, categories, collections, plants, guides, information pages; refreshed hourly;
  a new review updates a plant's date), robots rules (private and per-person pages are excluded),
  a default sharing picture and a favicon, and structured data (Store, WebSite search, Product with
  offers, ratings and reviews, BreadcrumbList, Article for guides).
- The first page of reviews is cached by the website for 60 seconds, so a new review can take up to
  a minute to appear for other visitors (the writer sees it at once).
