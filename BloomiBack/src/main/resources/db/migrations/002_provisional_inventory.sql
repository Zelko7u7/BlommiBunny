-- Existing inventory keeps confirmed costs and nonprovisional data.
BEGIN;
ALTER TABLE product ADD COLUMN IF NOT EXISTS provisional_data boolean NOT NULL DEFAULT false;
ALTER TABLE product ADD COLUMN IF NOT EXISTS cost_confirmed boolean NOT NULL DEFAULT true;
COMMIT;
