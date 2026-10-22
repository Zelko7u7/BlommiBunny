-- Removing a product hides it from inventory and store while keeping sale/stock history.
BEGIN;
ALTER TABLE product ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;
COMMIT;
