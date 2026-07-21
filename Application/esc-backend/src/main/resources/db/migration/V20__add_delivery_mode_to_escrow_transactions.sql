ALTER TABLE escrow_transactions
ADD COLUMN IF NOT EXISTS delivery_mode VARCHAR(30);
UPDATE escrow_transactions
SET delivery_mode = 'RIDER_REQUIRED'
WHERE delivery_mode IS NULL
    OR delivery_mode = '';
ALTER TABLE escrow_transactions
ALTER COLUMN delivery_mode
SET DEFAULT 'RIDER_REQUIRED';
ALTER TABLE escrow_transactions
ALTER COLUMN delivery_mode
SET NOT NULL;