ALTER TABLE olive_images
    ADD COLUMN IF NOT EXISTS olive_weight_kg NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS predicted_oil_kg NUMERIC(10, 2);
