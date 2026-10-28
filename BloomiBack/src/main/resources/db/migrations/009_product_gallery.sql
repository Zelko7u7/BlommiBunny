-- Secondary photos only. The existing product.image_url remains the cover.
-- Apply after a backup, before starting the version that maps this collection.
BEGIN;
CREATE TABLE IF NOT EXISTS product_image (
 product_id BIGINT NOT NULL REFERENCES product(id),
 sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
 image_url VARCHAR(500) NOT NULL,
 PRIMARY KEY (product_id, sort_order)
);
COMMIT;
