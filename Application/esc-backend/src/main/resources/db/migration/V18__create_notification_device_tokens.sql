-- Add RIDER role support
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users
ADD CONSTRAINT chk_users_role CHECK (
        role IN (
            'BUYER',
            'SELLER',
            'ADMIN',
            'SUPER_ADMIN',
            'RIDER'
        )
    );
-- Optional assignment handle from transaction side
ALTER TABLE escrow_transactions
ADD COLUMN IF NOT EXISTS rider_id UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_escrow_transactions_rider_id ON escrow_transactions(rider_id);
-- Device token registry for push notifications
CREATE TABLE notification_device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_device_tokens_user_active ON notification_device_tokens(user_id, active);
-- In-app notifications for notification center / history
CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    reference_id UUID,
    reference_type VARCHAR(60),
    payload_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    CONSTRAINT chk_in_app_notifications_status CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED'))
);
CREATE INDEX idx_in_app_notifications_user_created ON in_app_notifications(user_id, created_at DESC);
CREATE INDEX idx_in_app_notifications_user_status ON in_app_notifications(user_id, status);
-- Notification channel delivery attempts and outcomes
CREATE TABLE notification_delivery_logs (
    id UUID PRIMARY KEY,
    notification_id UUID REFERENCES in_app_notifications(id) ON DELETE
    SET NULL,
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        channel VARCHAR(20) NOT NULL,
        provider VARCHAR(40),
        provider_message_id VARCHAR(200),
        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        error_message TEXT,
        attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        delivered_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        CONSTRAINT chk_notification_delivery_logs_channel CHECK (channel IN ('PUSH', 'IN_APP', 'EMAIL', 'SMS')),
        CONSTRAINT chk_notification_delivery_logs_status CHECK (
            status IN (
                'PENDING',
                'SENT',
                'FAILED',
                'RETRYING',
                'DROPPED'
            )
        )
);
CREATE INDEX idx_notification_delivery_logs_user_channel ON notification_delivery_logs(user_id, channel, attempted_at DESC);
-- Rider profile and operational status
CREATE TABLE rider_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(150),
    phone VARCHAR(20),
    operation_area VARCHAR(160),
    license_number VARCHAR(80),
    vehicle_type VARCHAR(40),
    vehicle_plate VARCHAR(40),
    rider_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rider_profiles_status CHECK (
        rider_status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'SUSPENDED')
    )
);
-- Assignment workflow between transaction and rider
CREATE TABLE delivery_assignments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id) ON DELETE CASCADE,
    rider_user_id UUID NOT NULL REFERENCES users(id),
    assigned_by_user_id UUID REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    pickup_address TEXT,
    dropoff_address TEXT,
    pickup_due_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    arrived_at_buyer_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    rider_marked_delivered_at TIMESTAMPTZ,
    seller_confirmed_delivered_at TIMESTAMPTZ,
    buyer_confirmed_delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_delivery_assignments_status CHECK (
        status IN (
            'ASSIGNED',
            'ACCEPTED',
            'PICKED_UP',
            'IN_TRANSIT',
            'ARRIVED_AT_BUYER',
            'DELIVERED_TO_BUYER',
            'FAILED',
            'CANCELLED'
        )
    )
);
CREATE INDEX idx_delivery_assignments_transaction ON delivery_assignments(transaction_id);
CREATE INDEX idx_delivery_assignments_rider_status ON delivery_assignments(rider_user_id, status);
CREATE INDEX idx_delivery_assignments_transaction_status ON delivery_assignments(transaction_id, status);
-- Event timeline for transportation and handover process
CREATE TABLE delivery_tracking_events (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES delivery_assignments(id) ON DELETE CASCADE,
    transaction_id UUID NOT NULL REFERENCES escrow_transactions(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id),
    event_type VARCHAR(40) NOT NULL,
    event_note TEXT,
    event_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_delivery_tracking_event_type CHECK (
        event_type IN (
            'ASSIGNED',
            'ACCEPTED',
            'PICKUP_OTP_SENT',
            'PICKUP_OTP_VERIFIED',
            'PICKED_UP',
            'IN_TRANSIT',
            'ARRIVED_AT_BUYER',
            'DELIVERY_OTP_SENT',
            'DELIVERY_OTP_VERIFIED',
            'DELIVERY_CONFIRMED',
            'FAILED',
            'CANCELLED',
            'NOTE'
        )
    )
);
CREATE INDEX idx_delivery_tracking_events_assignment_created ON delivery_tracking_events(assignment_id, created_at DESC);
-- OTP confirmations for pickup and delivery handover (buyer + rider/seller)
CREATE TABLE delivery_confirmation_otps (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES delivery_assignments(id) ON DELETE CASCADE,
    recipient_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_role VARCHAR(20) NOT NULL,
    otp_purpose VARCHAR(40) NOT NULL,
    otp_hash TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_delivery_confirmation_otps_role CHECK (recipient_role IN ('BUYER', 'SELLER', 'RIDER')),
    CONSTRAINT chk_delivery_confirmation_otps_purpose CHECK (
        otp_purpose IN ('PICKUP_CONFIRMATION', 'DELIVERY_CONFIRMATION')
    ),
    CONSTRAINT chk_delivery_confirmation_otps_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'CANCELLED')
    )
);
CREATE INDEX idx_delivery_confirmation_otps_assignment_status ON delivery_confirmation_otps(assignment_id, status);
CREATE INDEX idx_delivery_confirmation_otps_recipient ON delivery_confirmation_otps(recipient_user_id, status);
-- Per-assignment confirmation summary to support seller and buyer OTP gates
CREATE TABLE delivery_handover_confirmations (
    assignment_id UUID PRIMARY KEY REFERENCES delivery_assignments(id) ON DELETE CASCADE,
    seller_user_id UUID REFERENCES users(id),
    buyer_user_id UUID REFERENCES users(id),
    seller_otp_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    buyer_otp_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    seller_otp_verified_at TIMESTAMPTZ,
    buyer_otp_verified_at TIMESTAMPTZ,
    seller_marked_delivered_at TIMESTAMPTZ,
    buyer_confirmed_delivery_at TIMESTAMPTZ,
    final_delivery_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_delivery_handover_confirmations_seller_status CHECK (
        seller_otp_status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED')
    ),
    CONSTRAINT chk_delivery_handover_confirmations_buyer_status CHECK (
        buyer_otp_status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED')
    ),
    CONSTRAINT chk_delivery_handover_confirmations_final_status CHECK (
        final_delivery_status IN (
            'IN_PROGRESS',
            'DELIVERED',
            'FAILED',
            'CANCELLED'
        )
    )
);