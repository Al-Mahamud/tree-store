-- Customer accounts (with a number proven by a texted code), saved addresses, wishlists, and the
-- text-message outbox and one-time codes that sign-up and password resets use.

create table customers (
    id                bigint generated always as identity primary key,
    name              text        not null,
    phone             text        not null unique check (phone ~ '^01[3-9][0-9]{8}$'),
    email             text,
    password_hash     text        not null,
    phone_verified_at timestamptz,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);

create table addresses (
    id          bigint generated always as identity primary key,
    customer_id bigint      not null references customers (id) on delete cascade,
    label       text        not null default 'Home',
    name        text        not null,
    phone       text        not null check (phone ~ '^01[3-9][0-9]{8}$'),
    address     text        not null,
    area        text        not null,
    city        text        not null,
    zone        text        not null check (zone in ('DHAKA', 'OUTSIDE')),
    note        text,
    is_default  boolean     not null default false,
    created_at  timestamptz not null default now()
);
create index addresses_customer on addresses (customer_id);
create unique index addresses_one_default on addresses (customer_id) where is_default;

-- Plants saved for later.
create table wishlist_items (
    customer_id bigint      not null references customers (id) on delete cascade,
    product_id  bigint      not null references products (id) on delete cascade,
    created_at  timestamptz not null default now(),
    primary key (customer_id, product_id)
);

-- A signed-in customer has one bag, shared by every device they sign in on.
alter table carts add constraint carts_customer_fk foreign key (customer_id) references customers (id) on delete cascade;
create unique index carts_one_per_customer on carts (customer_id) where customer_id is not null;
alter table orders add constraint orders_customer_fk foreign key (customer_id) references customers (id) on delete set null;

create table sms_messages (
    id          bigint generated always as identity primary key,
    phone       text        not null,
    kind        text        not null check (kind in ('CODE', 'ORDER_PLACED', 'ORDER_SHIPPED', 'ORDER_CANCELLED', 'CARE_REMINDER')),
    body        text        not null,
    -- Codes are masked in the body once sent, so they can't be read back later.
    sensitive   boolean     not null default false,
    order_id    bigint references orders (id) on delete set null,
    status      text        not null default 'QUEUED' check (status in ('QUEUED', 'SENDING', 'SENT', 'FAILED')),
    attempts    integer     not null default 0,
    provider    text,
    provider_id text,
    error       text,
    created_at  timestamptz not null default now(),
    sent_at     timestamptz
);
create index sms_queue on sms_messages (created_at) where status in ('QUEUED', 'SENDING');
create index sms_recent on sms_messages (created_at desc);

create table phone_codes (
    id         bigint generated always as identity primary key,
    phone      text        not null,
    purpose    text        not null check (purpose in ('REGISTER', 'RESET')),
    code_hash  text        not null,
    attempts   integer     not null default 0,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);
create index phone_codes_lookup on phone_codes (phone, purpose, created_at desc);
