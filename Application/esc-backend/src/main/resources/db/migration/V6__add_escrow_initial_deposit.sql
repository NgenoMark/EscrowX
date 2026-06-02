ALTER TABLE escrow_transactions
    ADD COLUMN initial_deposit_amount NUMERIC(18,2);

UPDATE escrow_transactions
SET initial_deposit_amount = amount
WHERE initial_deposit_amount IS NULL;

ALTER TABLE escrow_transactions
    ALTER COLUMN initial_deposit_amount SET NOT NULL,
    ADD CONSTRAINT chk_escrow_initial_deposit_amount
        CHECK (initial_deposit_amount > 0 AND initial_deposit_amount <= amount);
