-- Existing products start at version 0. Hibernate advances it when the product changes.
BEGIN;
ALTER TABLE product ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
COMMIT;
