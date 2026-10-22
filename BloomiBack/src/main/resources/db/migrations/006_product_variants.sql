ALTER TABLE product ADD COLUMN IF NOT EXISTS variant_group VARCHAR(80);
CREATE INDEX IF NOT EXISTS idx_product_variant_group ON product(variant_group);
