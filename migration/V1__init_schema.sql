-- EscrowX MVP initial schema

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('BUYER', 'SELLER', 'ADMIN', 'SUPER_ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BLACKLISTED'))
);

CREATE TABLE profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    display_name VARCHAR(150),
    business_name VARCHAR(150),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE escrow_transactions (
    id UUID PRIMARY KEY,
    reference VARCHAR(40) NOT NULL UNIQUE,
    buyer_id UUID NOT NULL REFERENCES users(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'KES',
    status VARCHAR(30) NOT NULL,
    delivery_due_at TIMESTAMPTZ,
    auto_release_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_escrow_status CHECK (
        status IN (
            'CREATED', 'PENDING_PAYMENT', 'FUNDS_HELD', 'SELLER_ACCEPTED', 'IN_DELIVERY',
            'DELIVERED', 'COMPLETED', 'DISPUTED', 'REFUNDED', 'CANCELLED', 'EXPIRED'
        )
    ),
    CONSTRAINT chk_escrow_buyer_seller_diff CHECK (buyer_id <> seller_id)
);

CREATE TABLE transaction_status_history (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users(id),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_intents (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE REFERENCES escrow_transactions(id),
    provider VARCHAR(20) NOT NULL,
    provider_ref VARCHAR(120),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_provider CHECK (provider IN ('MPESA')),
    CONSTRAINT chk_payment_status CHECK (status IN ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE TABLE payment_callbacks (
    id UUID PRIMARY KEY,
    payment_intent_id UUID NOT NULL REFERENCES payment_intents(id),
    provider_event_id VARCHAR(120) NOT NULL UNIQUE,
    raw_payload JSONB NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE disputes (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE REFERENCES escrow_transactions(id),
    raised_by UUID NOT NULL REFERENCES users(id),
    category VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_admin_id UUID REFERENCES users(id),
    resolution TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dispute_category CHECK (
        category IN ('NON_DELIVERY', 'DEFECTIVE_ITEM', 'NOT_AS_DESCRIBED', 'NON_PAYMENT', 'OTHER')
    ),
    CONSTRAINT chk_dispute_status CHECK (
        status IN ('OPEN', 'UNDER_REVIEW', 'ACTION_REQUIRED', 'RESOLVED', 'REJECTED')
    )
);

CREATE TABLE dispute_evidence (
    id UUID PRIMARY KEY,
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    file_url TEXT NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    channel VARCHAR(20) NOT NULL,
    type VARCHAR(60) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

CREATE INDEX idx_escrow_transactions_buyer_id ON escrow_transactions(buyer_id);
CREATE INDEX idx_escrow_transactions_seller_id ON escrow_transactions(seller_id);
CREATE INDEX idx_escrow_transactions_status ON escrow_transactions(status);
CREATE INDEX idx_escrow_transactions_auto_release_at ON escrow_transactions(auto_release_at);

CREATE INDEX idx_transaction_status_history_transaction_id
    ON transaction_status_history(transaction_id);

CREATE INDEX idx_payment_intents_status ON payment_intents(status);
CREATE INDEX idx_payment_callbacks_payment_intent_id ON payment_callbacks(payment_intent_id);

CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_assigned_admin_id ON disputes(assigned_admin_id);
CREATE INDEX idx_dispute_evidence_dispute_id ON dispute_evidence(dispute_id);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);

CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_entity_type_entity_id ON audit_logs(entity_type, entity_id);
