BEGIN;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;

ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN (
        'PENDING_VERIFICATION',
        'PENDING_ADMIN_APPROVAL',
        'ACTIVE',
        'SUSPENDED',
        'BLACKLISTED'
    ));

COMMIT;
