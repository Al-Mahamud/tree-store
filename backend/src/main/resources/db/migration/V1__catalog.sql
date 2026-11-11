-- Catalog: categories, collections and plants. Each plant comes in sizes and planters; every
-- size x planter pair is a variant with its own price and stock. Prices are whole taka.

create table categories (
    id          bigint generated always as identity primary key,
    parent_id   bigint references categories (id),
    name        text    not null,
    slug        text    not null unique,
    description text,
    image_url   text,
    position    integer not null default 0
);
create index categories_parent on categories (parent_id);

create table collections (
    id          bigint generated always as identity primary key,
    name        text    not null,
    slug        text    not null unique,
    tagline     text,
    description text,
    image_url   text,
    position    integer not null default 0,
    active      boolean not null default true
);

create table products (
    id             bigint generated always as identity primary key,
    name           text        not null,
    local_name     text,                       -- Bangla name, e.g. আম
    botanical_name text,
    slug           text        not null unique,
    category_id    bigint      not null references categories (id),
    description    text        not null default '',
    details        text        not null default '',  -- one bullet per line
    light          text        not null check (light in ('LOW', 'MEDIUM', 'BRIGHT', 'FULL_SUN')),
    water          text        not null check (water in ('LOW', 'MEDIUM', 'HIGH')),
    care_level     text        not null check (care_level in ('EASY', 'MODERATE', 'EXPERT')),
    placement      text        not null check (placement in ('INDOOR', 'OUTDOOR', 'BOTH')),
    pet_friendly   boolean     not null default false,
    mature_height  text,                       -- "8–10 m", "60–90 cm"
    growth_rate    text check (growth_rate in ('SLOW', 'MEDIUM', 'FAST')),
    fruiting       text,                       -- "Fruits in 2–3 years"; null when not a fruit plant
    status         text        not null default 'DRAFT' check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    featured       boolean     not null default false,
    sold_count     integer     not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    search         tsvector generated always as (
        setweight(to_tsvector('simple', name), 'A') ||
        setweight(to_tsvector('simple', coalesce(local_name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(botanical_name, '')), 'B') ||
        setweight(to_tsvector('simple', description), 'C')) stored
);
create index products_category on products (category_id);
create index products_status_created on products (status, created_at desc);
create index products_search on products using gin (search);

create table product_collections (
    product_id    bigint not null references products (id) on delete cascade,
    collection_id bigint not null references collections (id) on delete cascade,
    primary key (product_id, collection_id)
);
create index product_collections_collection on product_collections (collection_id);

create table planters (
    id         bigint generated always as identity primary key,
    product_id bigint  not null references products (id) on delete cascade,
    name       text    not null,
    kind       text    not null check (kind in ('GROW_BAG', 'CLAY', 'CERAMIC')),
    hex        text    not null check (hex ~ '^#[0-9A-Fa-f]{6}$'),
    position   integer not null default 0,
    unique (product_id, name)
);

create table product_images (
    id         bigint generated always as identity primary key,
    product_id bigint  not null references products (id) on delete cascade,
    planter_id bigint references planters (id) on delete cascade,
    url        text    not null,
    alt        text    not null default '',
    position   integer not null default 0
);
create index product_images_product on product_images (product_id, position);

create table variants (
    id               bigint generated always as identity primary key,
    product_id       bigint  not null references products (id) on delete cascade,
    planter_id       bigint  not null references planters (id) on delete cascade,
    size             text    not null check (size in ('SMALL', 'MEDIUM', 'LARGE')),
    height           text    not null,         -- what the customer receives: "1–2 ft"
    sku              text    not null unique,
    price            integer not null check (price > 0),
    compare_at_price integer check (compare_at_price is null or compare_at_price > price),
    stock            integer not null default 0 check (stock >= 0),
    dhaka_only       boolean not null default false,  -- too big to send outside Dhaka safely
    unique (product_id, planter_id, size)
);
create index variants_product on variants (product_id);
