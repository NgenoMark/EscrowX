ALTER TABLE escrow_transactions ADD COLUMN delivery_address TEXT;

-- Step 2: Backfill old transactions with a placeholder
UPDATE escrow_transactions SET delivery_address = 'Not Provided' WHERE delivery_address IS NULL;

-- Step 3: Enforce strict NOT NULL so future transactions CANNOT be empty
ALTER TABLE escrow_transactions ALTER COLUMN delivery_address SET NOT NULL;