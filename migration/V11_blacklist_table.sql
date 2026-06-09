-- 1. Start a transaction to ensure atomic execution
BEGIN;

-- 2. Drop the old user status check constraint so we can update/add rules if necessary
ALTER TABLE users DROP CONSTRAINT chk_users_status;

-- 3. Re-add the user status check constraint (keeping it flexible or updating values if needed)
ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BLACKLISTED'));

-- 4. Add the blacklist_status column to the users table
-- We default it to 'NOT_BLACKLISTED' so existing records migrate cleanly without breaking
ALTER TABLE users ADD COLUMN blacklist_status VARCHAR(40) NOT NULL DEFAULT 'NOT_BLACKLISTED';

-- 5. Add a check constraint for the new blacklist_status column
ALTER TABLE users ADD CONSTRAINT chk_users_blacklist_status
    CHECK (blacklist_status IN ('NOT_BLACKLISTED', 'TEMPORARILY_MUTED', 'PERMANENTLY_BANNED', 'UNDER_INVESTIGATION'));

-- 6. Create the new blacklists details table
CREATE TABLE user_blacklists (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                 blacklist_type VARCHAR(40) NOT NULL,
                                 reason TEXT NOT NULL,
                                 evidence_summary TEXT,
                                 blacklisted_by UUID NOT NULL REFERENCES users(id),
                                 expires_at TIMESTAMPTZ, -- NULL means indefinite/permanent ban
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT chk_blacklist_type CHECK (blacklist_type IN ('TEMPORARY', 'PERMANENT', 'INVESTIGATION'))
);

-- 7. Add optimal indexing for quick checks during login or security filtering
CREATE INDEX idx_users_blacklist_status ON users(blacklist_status);
CREATE INDEX idx_user_blacklists_user_id ON user_blacklists(user_id);
CREATE INDEX idx_user_blacklists_expires_at ON user_blacklists(expires_at);

-- Commit the changes
COMMIT;