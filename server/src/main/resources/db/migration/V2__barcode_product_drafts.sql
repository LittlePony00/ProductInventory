alter table products add column if not exists barcode varchar(255);
alter table products add column if not exists brand varchar(255);
alter table products add column if not exists ingredients text;
alter table products add column if not exists calories_kcal double precision;
alter table products add column if not exists protein_grams double precision;
alter table products add column if not exists fat_grams double precision;
alter table products add column if not exists carbohydrates_grams double precision;

create index if not exists idx_products_barcode on products (barcode);

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
