alter table user_food_preferences add column if not exists preferred_products_json text not null default '[]';
alter table user_food_preferences add column if not exists avoided_products_json text not null default '[]';

update categories set name = 'Молочные продукты' where system = true and code = 'DAIRY';
update categories set name = 'Мясо и рыба' where system = true and code = 'MEAT_FISH';
update categories set name = 'Овощи и фрукты' where system = true and code = 'VEGETABLES_FRUITS';
update categories set name = 'Крупы и злаки' where system = true and code = 'CEREALS';
update categories set name = 'Напитки' where system = true and code = 'BEVERAGES';
update categories set name = 'Другое' where system = true and code = 'OTHER';
