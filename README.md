# Shobuj — online tree and plant shop

An online nursery for Bangladesh: fruit, flowering, shade and timber trees, palms, indoor plants
and bonsai, in sizes and planters, delivered ready to plant.
See [docs/PLAN.md](docs/PLAN.md) for the full frontend and backend plan and the delivery phases.

## Layout

```text
backend/    Spring Boot API (Java 17, PostgreSQL, Flyway)
frontend/   Next.js storefront (TypeScript, Tailwind CSS)
docs/       Plan
```

## Backend

Needs JDK 17+. On this machine: `export JAVA_HOME=~/.jdks/jdk-17`.

```bash
cd backend
./mvnw verify                 # build and run all tests (embedded PostgreSQL, no Docker)
./mvnw spring-boot:test-run   # run on :8080 with a fresh database full of demo plants
```

Demo coupons: `GREEN10` (10% off over ৳1,000, up to ৳500), `MONSOON300` (৳300 off over ৳3,000),
`FREEDELIVERY` (free delivery over ৳2,000), `SPRING25` (expired, for testing).

Demo plant pictures are drawn by `backend/src/test/java/.../support/DemoArt.java` and written to
`backend/storage/demo/`. Real photos are uploaded in the admin area (Plants → a plant → Pictures).

## Frontend

Needs Node.js 20.9+. The backend must be running; Next.js forwards `/api/v1` and `/media` to it
(`BACKEND_URL`, default `http://localhost:8080`).

```bash
cd frontend
npm install
npm run dev       # http://localhost:3000
```

Demo account: mobile `01755555555`, password `shobuj-demo` (Nadia, with a saved address, two past
orders and three saved plants). She has reviewed her mango; her other delivered plants wait under
Account → Reviews. Sign-up codes, order texts and care texts in local runs appear at
http://localhost:8080/api/v1/test-texts (texts are only logged).

Admin area: http://localhost:3000/admin, mobile `01999999999`, password `shobuj-admin-1` (Farhana,
the nursery manager). Local runs start with a few open orders to confirm and send. On a real shop
the first staff account comes from `ADMIN_PHONE` and `ADMIN_PASSWORD` (at least 10 characters) when
there is none; add the rest of the team under Team.

Online payment in local runs goes to a fake SSLCommerz page (test sources only, no real money).
For the real sandbox, set `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD` and a public `WEB_URL`.

Texts: `SMS_DRIVER=log` (default) only logs them; `SMS_DRIVER=sslwireless` with
`SSLWIRELESS_API_TOKEN` and `SSLWIRELESS_SID` sends them for real.

Search engines: set `NEXT_PUBLIC_SITE_URL` to the shop's public address (used in canonical links,
the sitemap at `/sitemap.xml`, `/robots.txt` and shared-link pictures). Planting guides live in
`frontend/src/config/guides.ts` and the footer's information pages in `frontend/src/config/pages.ts`;
both are drafts to check before launch.
