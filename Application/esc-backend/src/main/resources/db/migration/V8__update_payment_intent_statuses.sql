ALTER TABLE payment_intents
    DROP CONSTRAINT chk_payment_status;

ALTER TABLE payment_intents
    ADD CONSTRAINT chk_payment_status CHECK (
        status IN ('INITIATED', 'PENDING', 'PAID', 'FAILED', 'CANCELLED')
    );
