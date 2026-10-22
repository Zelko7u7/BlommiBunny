-- Existing databases created before CUSTOMER was added need this migration once.
-- Fresh databases receive all three roles from the current Hibernate entity.
-- Run with psql -v ON_ERROR_STOP=1 -f this-file.sql against bloomidb.
BEGIN;
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check
    CHECK (role IN ('ADMIN', 'CUSTOMER', 'EMPLOYEE'));
COMMIT;
