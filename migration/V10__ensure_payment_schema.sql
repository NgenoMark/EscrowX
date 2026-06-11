CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id),
    buyer_id UUID REFERENCES users(id),
    seller_id UUID REFERENCES users(id),
    provider VARCHAR(20) NOT NULL,
    provider_ref VARCHAR(120),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(30),
    phone_number VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    checkout_request_id VARCHAR(120),
    merchant_request_id VARCHAR(120),
    mpesa_receipt_number VARCHAR(120),
    provider_response_code VARCHAR(30),
    provider_response_description TEXT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE payment_intents
    ADD COLUMN IF NOT EXISTS buyer_id UUID,
    ADD COLUMN IF NOT EXISTS seller_id UUID,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS checkout_request_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS merchant_request_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS mpesa_receipt_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS provider_response_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_response_description TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

UPDATE payment_intents
SET currency = 'KES'
WHERE currency IS NULL;

ALTER TABLE payment_intents
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN currency DROP DEFAULT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_provider'
    ) THEN
        ALTER TABLE payment_intents
            ADD CONSTRAINT chk_payment_provider CHECK (provider IN ('MPESA'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_status'
    ) THEN
        ALTER TABLE payment_intents
            ADD CONSTRAINT chk_payment_status CHECK (status IN ('INITIATED', 'PENDING', 'PAID', 'FAILED', 'CANCELLED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_intents_transaction'
    ) THEN
        ALTER TABLE payment_intents
            ADD CONSTRAINT fk_payment_intents_transaction FOREIGN KEY (transaction_id) REFERENCES escrow_transactions(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_intents_buyer'
    ) THEN
        ALTER TABLE payment_intents
            ADD CONSTRAINT fk_payment_intents_buyer FOREIGN KEY (buyer_id) REFERENCES users(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_intents_seller'
    ) THEN
        ALTER TABLE payment_intents
            ADD CONSTRAINT fk_payment_intents_seller FOREIGN KEY (seller_id) REFERENCES users(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_intents_transaction_id
    ON payment_intents(transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_intents_checkout_request_id
    ON payment_intents(checkout_request_id)
    WHERE checkout_request_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_intents_mpesa_receipt_number
    ON payment_intents(mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_intents_status ON payment_intents(status);
CREATE INDEX IF NOT EXISTS idx_payment_intents_buyer_id ON payment_intents(buyer_id);
CREATE INDEX IF NOT EXISTS idx_payment_intents_seller_id ON payment_intents(seller_id);

CREATE TABLE IF NOT EXISTS payouts (
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE payouts
    ADD COLUMN IF NOT EXISTS transaction_id UUID,
    ADD COLUMN IF NOT EXISTS seller_id UUID,
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS amount NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS seller_phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS conversation_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS originator_conversation_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS result_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS result_description TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

UPDATE payouts
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE payouts
SET updated_at = NOW()
WHERE updated_at IS NULL;

ALTER TABLE payouts
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN seller_id SET NOT NULL,
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN amount SET NOT NULL,
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN seller_phone_number SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payout_provider'
    ) THEN
        ALTER TABLE payouts
            ADD CONSTRAINT chk_payout_provider CHECK (provider IN ('MPESA'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payout_status'
    ) THEN
        ALTER TABLE payouts
            ADD CONSTRAINT chk_payout_status CHECK (status IN ('INITIATED', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payout_amount'
    ) THEN
        ALTER TABLE payouts
            ADD CONSTRAINT chk_payout_amount CHECK (amount > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payouts_transaction'
    ) THEN
        ALTER TABLE payouts
            ADD CONSTRAINT fk_payouts_transaction FOREIGN KEY (transaction_id) REFERENCES escrow_transactions(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payouts_seller'
    ) THEN
        ALTER TABLE payouts
            ADD CONSTRAINT fk_payouts_seller FOREIGN KEY (seller_id) REFERENCES users(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payouts_transaction_id_paid_or_processing
    ON payouts(transaction_id)
    WHERE status IN ('INITIATED', 'PROCESSING', 'PAID');

CREATE UNIQUE INDEX IF NOT EXISTS uq_payouts_conversation_id
    ON payouts(conversation_id)
    WHERE conversation_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payouts_originator_conversation_id
    ON payouts(originator_conversation_id)
    WHERE originator_conversation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payouts_transaction_id ON payouts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payouts_seller_id ON payouts(seller_id);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON payouts(status);

CREATE TABLE IF NOT EXISTS payment_callbacks (
    id UUID PRIMARY KEY,
    payment_intent_id UUID REFERENCES payment_intents(id),
    payout_id UUID REFERENCES payouts(id),
    provider_event_id VARCHAR(120),
    callback_type VARCHAR(40),
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE payment_callbacks
    ADD COLUMN IF NOT EXISTS payout_id UUID,
    ADD COLUMN IF NOT EXISTS callback_type VARCHAR(40);

UPDATE payment_callbacks
SET raw_payload = '{}'::jsonb
WHERE raw_payload IS NULL;

ALTER TABLE payment_callbacks
    ALTER COLUMN payment_intent_id DROP NOT NULL,
    ALTER COLUMN provider_event_id DROP NOT NULL,
    ALTER COLUMN raw_payload SET DEFAULT '{}'::jsonb,
    ALTER COLUMN raw_payload SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_callbacks_reference_present'
    ) THEN
        ALTER TABLE payment_callbacks
            ADD CONSTRAINT chk_payment_callbacks_reference_present
            CHECK (payment_intent_id IS NOT NULL OR payout_id IS NOT NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_callbacks_payment_intent'
    ) THEN
        ALTER TABLE payment_callbacks
            ADD CONSTRAINT fk_payment_callbacks_payment_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_callbacks_payout'
    ) THEN
        ALTER TABLE payment_callbacks
            ADD CONSTRAINT fk_payment_callbacks_payout FOREIGN KEY (payout_id) REFERENCES payouts(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payment_callbacks_payment_intent_id
    ON payment_callbacks(payment_intent_id);

CREATE INDEX IF NOT EXISTS idx_payment_callbacks_payout_id
    ON payment_callbacks(payout_id);

CREATE INDEX IF NOT EXISTS idx_payment_callbacks_callback_type
    ON payment_callbacks(callback_type);

CREATE TABLE IF NOT EXISTS escrow_ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id),
    entry_type VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE escrow_ledger_entries
    ADD COLUMN IF NOT EXISTS transaction_id UUID,
    ADD COLUMN IF NOT EXISTS entry_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS direction VARCHAR(10),
    ADD COLUMN IF NOT EXISTS amount NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS reference_id UUID,
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

UPDATE escrow_ledger_entries
SET created_at = NOW()
WHERE created_at IS NULL;

ALTER TABLE escrow_ledger_entries
    ALTER COLUMN transaction_id SET NOT NULL,
    ALTER COLUMN entry_type SET NOT NULL,
    ALTER COLUMN direction SET NOT NULL,
    ALTER COLUMN amount SET NOT NULL,
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_escrow_ledger_entry_type'
    ) THEN
        ALTER TABLE escrow_ledger_entries
            ADD CONSTRAINT chk_escrow_ledger_entry_type CHECK (entry_type IN ('HOLD', 'RELEASE', 'REFUND', 'FEE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_escrow_ledger_direction'
    ) THEN
        ALTER TABLE escrow_ledger_entries
            ADD CONSTRAINT chk_escrow_ledger_direction CHECK (direction IN ('CREDIT', 'DEBIT'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_escrow_ledger_amount'
    ) THEN
        ALTER TABLE escrow_ledger_entries
            ADD CONSTRAINT chk_escrow_ledger_amount CHECK (amount > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_escrow_ledger_entries_transaction'
    ) THEN
        ALTER TABLE escrow_ledger_entries
            ADD CONSTRAINT fk_escrow_ledger_entries_transaction FOREIGN KEY (transaction_id) REFERENCES escrow_transactions(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_escrow_ledger_entries_transaction_id
    ON escrow_ledger_entries(transaction_id);

CREATE INDEX IF NOT EXISTS idx_escrow_ledger_entries_reference
    ON escrow_ledger_entries(reference_type, reference_id);
