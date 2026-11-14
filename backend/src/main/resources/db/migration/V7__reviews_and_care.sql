-- Phase 7: reviews from people who received the plant, and care texts after delivery.

create table reviews (
    id          bigint generated always as identity primary key,
    product_id  bigint      not null references products (id) on delete cascade,
    customer_id bigint      not null references customers (id) on delete cascade,
    -- The delivered order the plant came in (kept when the order is gone).
    order_id    bigint references orders (id) on delete set null,
    rating      smallint    not null check (rating between 1 and 5),
    title       text,
    body        text        not null,
    -- Shown with the review: "Nadia R." and what was bought, e.g. "Medium · Clay pot".
    author      text        not null,
    bought      text,
    status      text        not null default 'PUBLISHED' check (status in ('PUBLISHED', 'HIDDEN')),
    reply       text,
    replied_at  timestamptz,
    replied_by  text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (product_id, customer_id)
);
create index reviews_product on reviews (product_id, created_at desc) where status = 'PUBLISHED';
create index reviews_recent on reviews (created_at desc);

create table review_photos (
    id         bigint generated always as identity primary key,
    review_id  bigint  not null references reviews (id) on delete cascade,
    url        text    not null,
    position   integer not null default 0
);
create index review_photos_review on review_photos (review_id, position);

-- Kept up to date from published reviews, so cards don't count reviews on every request.
alter table products add column rating numeric(2, 1), add column review_count integer not null default 0;

-- Care texts: on unless the shopper turns them off (at checkout or on the order page).
alter table orders add column care_texts boolean not null default true, add column delivered_at timestamptz;
update orders o set delivered_at = (select max(created_at) from order_events e where e.order_id = o.id and e.status = 'DELIVERED')
 where status = 'DELIVERED';

create table care_reminders (
    id       bigint generated always as identity primary key,
    order_id bigint      not null references orders (id) on delete cascade,
    step     smallint    not null check (step in (1, 2, 3)),
    due_at   timestamptz not null,
    done_at  timestamptz,
    -- SENT, or why it wasn't: OFF (turned off), LATE (more than two days overdue).
    outcome  text check (outcome in ('SENT', 'OFF', 'LATE')),
    unique (order_id, step)
);
create index care_reminders_due on care_reminders (due_at) where done_at is null;
