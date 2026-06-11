ALTER TABLE escrow_transactions
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING TRIM(currency);

ALTER TABLE escrow_transactions
    ALTER COLUMN currency SET DEFAULT 'KES';
