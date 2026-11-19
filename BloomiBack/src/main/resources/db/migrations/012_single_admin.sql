-- Back up first. Enforce at most one ADMIN, including inactive accounts.
-- Does not remove accounts, promote users or change credentials.
BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE app_user IN SHARE ROW EXCLUSIVE MODE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_single_admin
    ON app_user (role) WHERE role = 'ADMIN';
COMMIT;
