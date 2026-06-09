ALTER TABLE escrow_transactions
    DROP CONSTRAINT chk_escrow_status;

ALTER TABLE escrow_transactions
    ADD CONSTRAINT chk_escrow_status CHECK (
        status IN (
            'CREATED',
            'PENDING_PAYMENT',
            'FUNDS_HELD',
            'SELLER_ACCEPTED',
            'IN_DELIVERY',
            'DELIVERED',
            'RELEASE_PENDING',
            'RELEASE_PROCESSING',
            'RELEASE_FAILED',
            'COMPLETED',
            'DISPUTED',
            'REFUND_PENDING',
            'REFUND_PROCESSING',
            'REFUNDED',
            'CANCELLED',
            'EXPIRED'
        )
    );
