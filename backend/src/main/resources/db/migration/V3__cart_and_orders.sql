-- Carts, coupons and orders. customer_id columns get their foreign keys with the customers table (V4).

create table carts (
    id          uuid primary key,
    customer_id bigint,
    coupon_code text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create index carts_updated on carts (updated_at);

create table cart_items (
    cart_id    uuid    not null references carts (id) on delete cascade,
    variant_id bigint  not null references variants (id) on delete cascade,
    quantity   integer not null check (quantity between 1 and 10),
    added_at   timestamptz not null default now(),
    primary key (cart_id, variant_id)
);

create table coupons (
    id           bigint generated always as identity primary key,
    code         text        not null unique check (code = upper(code)),
    description  text        not null default '',
    kind         text        not null check (kind in ('PERCENT', 'FIXED', 'FREE_SHIPPING')),
    value        integer     not null default 0 check (value >= 0),
    max_discount integer check (max_discount is null or max_discount > 0),
    min_subtotal integer     not null default 0 check (min_subtotal >= 0),
    starts_at    timestamptz,
    ends_at      timestamptz,
    usage_limit  integer check (usage_limit is null or usage_limit > 0),
    used_count   integer     not null default 0,
    active       boolean     not null default true,
    created_at   timestamptz not null default now(),
    check (kind <> 'PERCENT' or value between 1 and 90)
);

create sequence order_number_seq start with 10001;

create table orders (
    id             bigint generated always as identity primary key,
    number         text        not null unique,
    access_token   text        not null,  -- lets a guest open their own order page
    customer_id    bigint,
    name           text        not null,
    phone          text        not null check (phone ~ '^01[3-9][0-9]{8}$'),
    email          text,
    address        text        not null,
    area           text        not null,
    city           text        not null,
    zone           text        not null check (zone in ('DHAKA', 'OUTSIDE')),
    note           text,
    subtotal       integer     not null check (subtotal >= 0),
    discount       integer     not null default 0 check (discount >= 0),
    shipping_fee   integer     not null check (shipping_fee >= 0),
    total          integer     not null check (total >= 0),
    coupon_code    text,
    payment_method text        not null check (payment_method in ('COD', 'ONLINE')),
    payment_status text        not null default 'UNPAID' check (payment_status in ('UNPAID', 'PAID', 'REFUNDED')),
    status         text        not null check (status in
        ('PENDING_PAYMENT', 'PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);
create index orders_phone on orders (phone);
create index orders_customer on orders (customer_id, created_at desc);
create index orders_status_created on orders (status, created_at desc);

-- Copies of what was bought, so editing a product never changes an old order.
create table order_items (
    id           bigint generated always as identity primary key,
    order_id     bigint  not null references orders (id) on delete cascade,
    variant_id   bigint references variants (id) on delete set null,
    product_id   bigint references products (id) on delete set null,
    product_slug text    not null,
    product_name text    not null,
    planter      text    not null,
    size         text    not null,
    height       text    not null,
    sku          text    not null,
    image_url    text,
    unit_price   integer not null check (unit_price >= 0),
    quantity     integer not null check (quantity > 0)
);
create index order_items_order on order_items (order_id);

create table order_events (
    id         bigint generated always as identity primary key,
    order_id   bigint      not null references orders (id) on delete cascade,
    status     text        not null,
    note       text,
    actor      text        not null default 'system',
    created_at timestamptz not null default now()
);
create index order_events_order on order_events (order_id, created_at);
