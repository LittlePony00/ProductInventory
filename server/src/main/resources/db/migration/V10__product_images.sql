ALTER TABLE products
    ADD COLUMN image_url TEXT;

ALTER TABLE products
    ADD COLUMN image_object_key TEXT;

ALTER TABLE barcode_product_cache
    ADD COLUMN image_url TEXT;
