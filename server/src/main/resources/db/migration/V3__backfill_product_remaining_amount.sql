update products
set remaining_amount = quantity
where remaining_amount is null;

alter table products
alter column remaining_amount set not null;
