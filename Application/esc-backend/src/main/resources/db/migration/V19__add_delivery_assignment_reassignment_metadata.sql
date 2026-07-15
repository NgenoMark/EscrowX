ALTER TABLE delivery_assignments
    ADD COLUMN IF NOT EXISTS previous_rider_user_id UUID;

ALTER TABLE delivery_assignments
    ADD COLUMN IF NOT EXISTS reassignment_reason VARCHAR(255);
