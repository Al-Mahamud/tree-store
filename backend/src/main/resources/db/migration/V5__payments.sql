-- Online payment through SSLCommerz: attempts per order, and every callback as received.

alter table orders add column payment_expires_at timestamptz;
alter table orders add column paid_at timestamptz;
create index orders_awaiting_payment on orders (payment_expires_at) where status = 'PENDING_PAYMENT';

create table payment_attempts (
    id           bigint generated always as identity primary key,
    order_id     bigint      not null references orders (id) on delete cascade,
    tran_id      text        not null unique,
    amount       integer     not null check (amount > 0),
    status       text        not null default 'PENDING'
                 check (status in ('PENDING', 'PAID', 'FAILED', 'CANCELLED')),
    val_id       text,
    bank_tran_id text,
    card_type    text,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create index payment_attempts_order on payment_attempts (order_id, created_at);
create index payment_attempts_pending on payment_attempts (created_at) where status = 'PENDING';

create table gateway_callbacks (
    id          bigint generated always as identity primary key,
    kind        text        not null,
    tran_id     text,
    body        text        not null,
    received_at timestamptz not null default now()
);
create index gateway_callbacks_tran on gateway_callbacks (tran_id);
