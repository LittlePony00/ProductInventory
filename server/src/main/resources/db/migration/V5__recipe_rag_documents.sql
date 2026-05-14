create table if not exists recipe_documents (
    id varchar(255) primary key,
    title varchar(255) not null,
    ingredients_json text not null,
    steps_json text not null,
    time_text varchar(255) not null,
    calories integer not null,
    required_ingredients text not null,
    categories text not null,
    rules_json text not null
);

insert into recipe_documents (
    id,
    title,
    ingredients_json,
    steps_json,
    time_text,
    calories,
    required_ingredients,
    categories,
    rules_json
) values
(
    'vegetable-rice-bowl',
    'Vegetable Rice Bowl',
    '[{"name":"rice","amount":"1 cup"},{"name":"vegetables","amount":"2 cups"},{"name":"oil","amount":"1 tbsp"}]',
    '["Cook rice until tender","Saute chopped vegetables","Mix rice with vegetables and season to taste"]',
    '25 minutes',
    430,
    'rice,vegetables',
    'CEREALS,VEGETABLES_FRUITS',
    '["Prefer vegetables expiring soon","Use cereals when stock is available"]'
),
(
    'dairy-breakfast-porridge',
    'Dairy Breakfast Porridge',
    '[{"name":"cereal","amount":"1 cup"},{"name":"milk","amount":"250 ml"},{"name":"fruit","amount":"1 piece"}]',
    '["Warm milk without boiling","Add cereal and simmer until soft","Top with chopped fruit"]',
    '15 minutes',
    360,
    'cereal,milk,fruit',
    'CEREALS,DAIRY,VEGETABLES_FRUITS',
    '["Use dairy before expiration","Prefer fruit with low remaining amount"]'
),
(
    'quick-fish-vegetables',
    'Quick Fish With Vegetables',
    '[{"name":"fish","amount":"200 g"},{"name":"vegetables","amount":"2 cups"},{"name":"lemon","amount":"1 tbsp"}]',
    '["Season fish","Pan-cook fish until done","Serve with steamed vegetables and lemon"]',
    '20 minutes',
    390,
    'fish,vegetables',
    'MEAT_FISH,VEGETABLES_FRUITS',
    '["Prioritize meat and fish close to expiration","Keep steps short for perishable products"]'
),
(
    'fruit-yogurt-smoothie',
    'Fruit Yogurt Smoothie',
    '[{"name":"yogurt","amount":"200 g"},{"name":"fruit","amount":"1 cup"},{"name":"beverage","amount":"100 ml"}]',
    '["Add yogurt and fruit to a blender","Blend until smooth","Thin with beverage if needed"]',
    '10 minutes',
    250,
    'yogurt,fruit',
    'DAIRY,VEGETABLES_FRUITS,BEVERAGES',
    '["Use expiring dairy first","Use fruit with small remaining stock"]'
),
(
    'simple-meat-cereal-skillet',
    'Simple Meat Cereal Skillet',
    '[{"name":"meat","amount":"200 g"},{"name":"cereal","amount":"1 cup"},{"name":"vegetables","amount":"1 cup"}]',
    '["Cook cereal according to package instructions","Brown meat thoroughly","Add vegetables and combine with cereal"]',
    '30 minutes',
    520,
    'meat,cereal,vegetables',
    'MEAT_FISH,CEREALS,VEGETABLES_FRUITS',
    '["Avoid recipes that require unavailable stock","Use meat and fish before other categories"]'
);
