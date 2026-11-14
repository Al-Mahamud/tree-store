-- The admin area: staff accounts, delivery fees the shop can change, and refunds of online payments.

create table staff (
    id             bigint generated always as identity primary key,
    name           text        not null,
    phone          text        not null unique check (phone ~ '^01[3-9][0-9]{8}$'),
    password_hash  text        not null,
    active         boolean     not null default true,
    created_at     timestamptz not null default now(),
    last_signed_in timestamptz
);

-- One row: delivery fees in taka. Until the shop saves its own, application.yml's fees apply.
create table delivery_settings (
    id           integer     primary key default 1 check (id = 1),
    inside_dhaka integer     not null check (inside_dhaka between 0 and 5000),
    outside_dhaka integer    not null check (outside_dhaka between 0 and 5000),
    free_over    integer     not null check (free_over between 0 and 1000000),
    updated_at   timestamptz not null default now(),
    updated_by   text
);

alter table orders add column refunded_at timestamptz;
alter table orders add column refund_reference text;
alter table orders add column courier text;          -- who carries an order sent outside Dhaka
alter table orders add column tracking_code text;    -- the courier's consignment number
create index orders_refund_due on orders (updated_at) where status = 'CANCELLED' and payment_status = 'PAID';
create index orders_created on orders (created_at desc);
create index variants_low_stock on variants (stock);

-- The editor saves all of a plant's options at once; checking size clashes at commit lets two
-- options swap sizes in one save.
alter table variants drop constraint variants_product_id_planter_id_size_key;
alter table variants add constraint variants_product_planter_size unique (product_id, planter_id, size)
    deferrable initially deferred;
