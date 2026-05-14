alter table products add column if not exists barcode varchar(255);
alter table products add column if not exists brand varchar(255);
alter table products add column if not exists package_amount double precision;
alter table products add column if not exists package_unit varchar(255);
alter table products add column if not exists ingredients_text text;
alter table products add column if not exists calories double precision;
alter table products add column if not exists protein double precision;
alter table products add column if not exists fat double precision;
alter table products add column if not exists carbs double precision;
alter table products add column if not exists purchase_date date;
alter table products add column if not exists remaining_amount double precision;
alter table products add column if not exists low_stock_threshold double precision;

create index if not exists idx_products_barcode on products (barcode);

create table if not exists barcode_products (
    id uuid primary key,
    barcode varchar(255) not null unique,
    name varchar(255) not null,
    category varchar(255),
    image_url varchar(255),
    fetched_at timestamp not null
);

create table if not exists barcode_product_cache (
    barcode varchar(255) primary key,
    name varchar(255),
    brand varchar(255),
    package_quantity double precision,
    package_quantity_unit varchar(255),
    ingredients text,
    calories_kcal double precision,
    protein_grams double precision,
    fat_grams double precision,
    carbohydrates_grams double precision,
    category varchar(255),
    source varchar(255) not null,
    confidence double precision not null,
    updated_at timestamp not null
);
