ALTER TABLE payment_intents
    ADD COLUMN IF NOT EXISTS buyer_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS seller_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS checkout_request_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS merchant_request_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS mpesa_receipt_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS provider_response_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_response_description TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

ALTER TABLE payment_intents
    ALTER COLUMN currency DROP DEFAULT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_intents_checkout_request_id
    ON payment_intents(checkout_request_id)
    WHERE checkout_request_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_intents_mpesa_receipt_number
    ON payment_intents(mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_intents_transaction_id
    ON payment_intents(transaction_id);

CREATE INDEX IF NOT EXISTS idx_payment_intents_buyer_id
    ON payment_intents(buyer_id);

CREATE INDEX IF NOT EXISTS idx_payment_intents_seller_id
    ON payment_intents(seller_id);

CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(20) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    seller_phone_number VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    conversation_id VARCHAR(120),
    originator_conversation_id VARCHAR(120),
    result_code VARCHAR(30),
    result_description TEXT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payout_provider CHECK (provider IN ('MPESA')),
    CONSTRAINT chk_payout_status CHECK (status IN ('INITIATED', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uq_payouts_transaction_id_paid_or_processing
    ON payouts(transaction_id)
    WHERE status IN ('INITIATED', 'PROCESSING', 'PAID');

CREATE UNIQUE INDEX uq_payouts_conversation_id
    ON payouts(conversation_id)
    WHERE conversation_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payouts_originator_conversation_id
    ON payouts(originator_conversation_id)
    WHERE originator_conversation_id IS NOT NULL;

CREATE INDEX idx_payouts_transaction_id ON payouts(transaction_id);
CREATE INDEX idx_payouts_seller_id ON payouts(seller_id);
CREATE INDEX idx_payouts_status ON payouts(status);

ALTER TABLE payment_callbacks
    ADD COLUMN IF NOT EXISTS payout_id UUID REFERENCES payouts(id),
    ADD COLUMN IF NOT EXISTS callback_type VARCHAR(40);

ALTER TABLE payment_callbacks
    ALTER COLUMN payment_intent_id DROP NOT NULL,
    ALTER COLUMN provider_event_id DROP NOT NULL,
    ALTER COLUMN raw_payload SET DEFAULT '{}'::jsonb;

ALTER TABLE payment_callbacks
    ADD CONSTRAINT chk_payment_callbacks_reference_present
    CHECK (payment_intent_id IS NOT NULL OR payout_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_payment_callbacks_payout_id
    ON payment_callbacks(payout_id);

CREATE INDEX IF NOT EXISTS idx_payment_callbacks_callback_type
    ON payment_callbacks(callback_type);

CREATE TABLE escrow_ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id),
    entry_type VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_escrow_ledger_entry_type CHECK (entry_type IN ('HOLD', 'RELEASE', 'REFUND', 'FEE')),
    CONSTRAINT chk_escrow_ledger_direction CHECK (direction IN ('CREDIT', 'DEBIT'))
);

CREATE INDEX idx_escrow_ledger_entries_transaction_id
    ON escrow_ledger_entries(transaction_id);

CREATE INDEX idx_escrow_ledger_entries_reference
    ON escrow_ledger_entries(reference_type, reference_id);
