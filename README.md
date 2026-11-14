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
`backend/storage/demo/`. Real photos will be uploaded through the admin (phase 6).

## Frontend

Needs Node.js 20.9+. The backend must be running; Next.js forwards `/api/v1` and `/media` to it
(`BACKEND_URL`, default `http://localhost:8080`).

```bash
cd frontend
npm install
npm run dev       # http://localhost:3000
```

Demo account: mobile `01755555555`, password `shobuj-demo` (Nadia, with a saved address, two past
orders and three saved plants). Sign-up codes in local runs appear at
http://localhost:8080/api/v1/test-texts (texts are only logged).
