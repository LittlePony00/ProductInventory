create table if not exists user_food_preferences (
    user_id uuid primary key,
    preferred_cuisines_json text not null default '[]',
    disliked_ingredients_json text not null default '[]',
    allergies_json text not null default '[]',
    dietary_restrictions_json text not null default '[]',
    max_cooking_time_minutes integer,
    preferred_difficulty varchar(32),
    servings integer
);
