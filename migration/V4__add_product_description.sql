ALTER TABLE escrow_transactions
    ADD COLUMN product_description VARCHAR(255);

UPDATE escrow_transactions
SET product_description = 'N/A'
WHERE product_description IS NULL;

ALTER TABLE escrow_transactions
    ALTER COLUMN product_description SET NOT NULL;