create table if not exists categories (
    id uuid primary key,
    household_id uuid,
    code varchar(255),
    name varchar(255) not null,
    system boolean not null,
    archived boolean not null,
    created_at timestamp not null
);

create index if not exists idx_categories_household_id on categories (household_id);
create index if not exists idx_categories_code on categories (code);

insert into categories (id, household_id, code, name, system, archived, created_at) values
(cast('00000000-0000-0000-0000-000000000101' as uuid), null, 'DAIRY', 'Dairy', true, false, timestamp '2026-01-01 00:00:00'),
(cast('00000000-0000-0000-0000-000000000102' as uuid), null, 'MEAT_FISH', 'Meat/Fish', true, false, timestamp '2026-01-01 00:00:00'),
(cast('00000000-0000-0000-0000-000000000103' as uuid), null, 'VEGETABLES_FRUITS', 'Vegetables/Fruits', true, false, timestamp '2026-01-01 00:00:00'),
(cast('00000000-0000-0000-0000-000000000104' as uuid), null, 'CEREALS', 'Cereals', true, false, timestamp '2026-01-01 00:00:00'),
(cast('00000000-0000-0000-0000-000000000105' as uuid), null, 'BEVERAGES', 'Beverages', true, false, timestamp '2026-01-01 00:00:00'),
(cast('00000000-0000-0000-0000-000000000106' as uuid), null, 'OTHER', 'Other', true, false, timestamp '2026-01-01 00:00:00');

alter table products add column if not exists category_id uuid;

update products
set category_id = case category
    when 'DAIRY' then cast('00000000-0000-0000-0000-000000000101' as uuid)
    when 'MEAT_FISH' then cast('00000000-0000-0000-0000-000000000102' as uuid)
    when 'VEGETABLES_FRUITS' then cast('00000000-0000-0000-0000-000000000103' as uuid)
    when 'CEREALS' then cast('00000000-0000-0000-0000-000000000104' as uuid)
    when 'BEVERAGES' then cast('00000000-0000-0000-0000-000000000105' as uuid)
    else cast('00000000-0000-0000-0000-000000000106' as uuid)
end
where category_id is null;

create index if not exists idx_products_category_id on products (category_id);
