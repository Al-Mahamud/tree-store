# Shobuj — putting the shop online

Everything runs on one Linux server with Docker: PostgreSQL, the API, the website, and Caddy,
which gets the HTTPS certificate and is the only part reachable from the internet
(`deploy/compose.yaml`). Every push to `main` runs the tests (`.github/workflows/ci.yml`), and
the **Deploy drill** workflow builds this exact setup on a throwaway machine, signs in, backs up,
wipes and restores it (`.github/workflows/deploy-drill.yml`).

## 1. What you need

| What | Notes |
|---|---|
| A server | Ubuntu with Docker and the compose plugin; 2 CPUs and 4 GB memory is plenty to start |
| A domain | An `A` record pointing at the server, e.g. `shobuj.example` |
| SSL Wireless account | An API token and an approved sender id (SID). Without texts nobody can sign up, and no order or care texts go out |
| SSLCommerz account | Store id and password. Optional: without it the shop is cash on delivery only |
| Off-site storage (recommended) | Any [rclone](https://rclone.org) remote for copies of the backups |

## 2. Before the first build

1. **Contact details and wording.** `frontend/src/config/site.ts` still has placeholder contact
   details (`+880 1700-000000`, `hello@shobuj.example`, the nursery address). The information pages
   (`frontend/src/config/pages.ts`) and planting guides (`frontend/src/config/guides.ts`) are drafts:
   check every promise and every piece of advice, and have Terms and Privacy read by a lawyer.
2. **Settings.** `cp deploy/.env.example deploy/.env` and fill it in (it holds passwords: keep it
   private, it is never committed):
   - `DOMAIN` — the shop's address, without `https://`. It also becomes the website's public
     address for search engines and shared links.
   - `POSTGRES_PASSWORD` — long and random: `openssl rand -base64 30`.
   - `SMS_DRIVER=sslwireless`, `SSLWIRELESS_API_TOKEN`, `SSLWIRELESS_SID`.
   - `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD`, and `SSLCOMMERZ_BASE_URL`: the sandbox
     until SSLCommerz approves the live store, then `https://securepay.sslcommerz.com`.
   - `ADMIN_PHONE`, `ADMIN_PASSWORD` (10+ characters) — the first staff account.

The API refuses to start with unsafe settings (an `http://` address, texts that only go to the log,
an empty database password) and says which one to fix.

## 3. First start

```sh
git clone https://github.com/Al-Mahamud/tree-store.git /srv/shobuj && cd /srv/shobuj
cp deploy/.env.example deploy/.env   # fill it in
docker compose -f deploy/compose.yaml --env-file deploy/.env up -d --build
docker compose -f deploy/compose.yaml --env-file deploy/.env ps   # all "healthy" after a minute or two
```

Then, at `https://<DOMAIN>/admin`:

1. Sign in with `ADMIN_PHONE` / `ADMIN_PASSWORD`, change your password under **Team → My password**,
   then delete the two `ADMIN_` lines from `deploy/.env`.
2. **Team**: add everyone who packs, sends out or answers the phone, each with their own login.
3. **Delivery**: set the inside-Dhaka and outside-Dhaka fees and the free-delivery amount. Until you
   save them, the defaults (৳100, ৳200, free over ৳5,000) apply.
4. **Categories**: a new shop starts with the nursery's usual categories (fruit, flowering, shade &
   timber trees, palms, indoor plants, bonsai). Rename, add or remove them, and give each a picture.
5. **Plants**: add each plant with its care facts, planters, the sizes it comes in (height, price,
   stock, and "inside Dhaka only" for large trees) and JPEG/PNG/WebP pictures. Set it to **On sale**
   when ready. The home page shows sections only once they have plants in them.
6. **Collections** (seasonal banners such as monsoon planting) and **Coupons**: none exist until you
   make them. The first collection becomes the home page banner.

## 4. Online payment

In the SSLCommerz merchant panel, set the IPN address to
`https://<DOMAIN>/api/v1/payments/sslcommerz/ipn`. Before switching to the live address, make one
real sandbox payment end to end (place an order, pay, see it confirmed in the admin). A paid order
that is cancelled shows as **Refund due**; refund it in the SSLCommerz panel and record the
reference on the order.

## 5. A test (staging) server

Same steps with `STAGING=true` and `SMS_DRIVER=log`: texts are only written to the log
(`docker compose ... logs backend | grep -i sms` shows sign-up codes), and the sandbox is fine.
Never let customers use a staging server.

## 6. Backups

`scripts/backup.sh` saves the database and the uploaded pictures (plants and review photos) to
`./backups`, checks the dump is readable, keeps 14 days, and copies to `BACKUP_REMOTE` if set. Run
it every night:

```sh
# crontab -e on the server (03:15 server time)
15 3 * * * cd /srv/shobuj && BACKUP_REMOTE=offsite:shobuj-backups scripts/backup.sh >> backups/backup.log 2>&1
```

To restore (the shop is down while it runs, and the current data is replaced):

```sh
scripts/restore.sh backups/db-<time>.dump backups/storage-<time>.tar.gz
```

The Deploy drill workflow does a full backup-wipe-restore every Monday; run it from the Actions tab
after changing anything under `deploy/` or `scripts/`.

## 7. Updates

```sh
cd /srv/shobuj && scripts/backup.sh && git pull \
  && docker compose -f deploy/compose.yaml --env-file deploy/.env up -d --build
```

Database changes (Flyway migrations) run by themselves when the API starts. Merge Dependabot's
weekly update proposals only when CI is green.

## 8. Keeping an eye on it

- `docker compose -f deploy/compose.yaml --env-file deploy/.env ps` — every service healthy.
- `... logs --tail=200 backend` — the API logs one JSON line per event.
- The admin **Text messages** page shows texts that failed, with a Retry button.
- The admin **Reviews** page, tab "1–2 stars", shows unhappy customers worth a call.
- An outside uptime check on `https://<DOMAIN>/robots.txt` tells you when the site is down.
- Tell search engines about `https://<DOMAIN>/sitemap.xml` (Google Search Console).

## 9. Not covered yet

- SSL Wireless was built from their published API and has not been tried with a real account:
  send yourself a sign-up code before launch.
- Refunds are recorded by hand (reference from the SSLCommerz panel), not sent through their API.
- No email: customers get texts only.
- One server: no automatic failover. Backups are the safety net; practise a restore.
- Docker isn't available on the machine this was built on, so the images and compose file were
  checked by the Deploy drill workflow on GitHub, not locally.
