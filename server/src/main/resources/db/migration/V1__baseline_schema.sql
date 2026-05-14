create table if not exists users (
    id uuid primary key,
    email varchar(255) not null unique,
    name varchar(255) not null,
    password_hash varchar(255) not null,
    created_at timestamp not null
);

create table if not exists households (
    id uuid primary key,
    name varchar(255) not null,
    created_at timestamp not null
);

create table if not exists memberships (
    id uuid primary key,
    user_id uuid not null,
    household_id uuid not null,
    role varchar(255) not null,
    joined_at timestamp not null,
    constraint uk_memberships_user_household unique (user_id, household_id)
);

create table if not exists products (
    id uuid primary key,
    name varchar(255) not null,
    brand varchar(255),
    barcode varchar(255),
    category varchar(255) not null,
    quantity double precision not null,
    quantity_unit varchar(255) not null,
    package_amount double precision,
    package_unit varchar(255),
    ingredients_text text,
    calories double precision,
    protein double precision,
    fat double precision,
    carbs double precision,
    purchase_date date,
    remaining_amount double precision,
    low_stock_threshold double precision,
    expiration_date date,
    household_id uuid not null,
    added_by_user_id uuid not null,
    created_at timestamp not null
);

create index if not exists idx_products_barcode on products (barcode);

create table if not exists barcode_products (
    id uuid primary key,
    barcode varchar(255) not null unique,
    name varchar(255) not null,
    category varchar(255),
    image_url varchar(255),
    fetched_at timestamp not null
);

create table if not exists notifications (
    id uuid primary key,
    user_id uuid not null,
    title varchar(255) not null,
    message text not null,
    sent_at timestamp not null,
    is_read boolean not null
);

create table if not exists invite_codes (
    id uuid primary key,
    code varchar(255) not null unique,
    household_id uuid not null,
    created_by_user_id uuid not null,
    created_at timestamp not null,
    expires_at timestamp not null,
    used boolean not null
);

create table if not exists refresh_tokens (
    id uuid primary key,
    token varchar(255) not null unique,
    user_id uuid not null,
    expires_at timestamp not null,
    revoked boolean not null
);
