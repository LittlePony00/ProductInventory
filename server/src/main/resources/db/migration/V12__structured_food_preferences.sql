alter table user_food_preferences add column if not exists preferred_product_ids_json text not null default '[]';
alter table user_food_preferences add column if not exists avoided_product_ids_json text not null default '[]';
alter table user_food_preferences add column if not exists preferred_category_ids_json text not null default '[]';
alter table user_food_preferences add column if not exists avoided_category_ids_json text not null default '[]';
