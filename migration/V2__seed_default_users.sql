-- Seed default users for each role (MVP bootstrap)
-- Password hash below corresponds to: "password"
-- Change these credentials immediately in non-local environments.

INSERT INTO users (id, email, phone, password_hash, role, status, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'buyer@escrowx.local', '+254700000001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi.6q9d7cQx6g9Q8Y4M8zQxqfQ9Rz5K', 'BUYER', 'ACTIVE', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'seller@escrowx.local', '+254700000002', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi.6q9d7cQx6g9Q8Y4M8zQxqfQ9Rz5K', 'SELLER', 'ACTIVE', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'admin@escrowx.local', '+254700000003', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi.6q9d7cQx6g9Q8Y4M8zQxqfQ9Rz5K', 'ADMIN', 'ACTIVE', NOW(), NOW()),
    ('44444444-4444-4444-4444-444444444444', 'superadmin@escrowx.local', '+254700000004', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi.6q9d7cQx6g9Q8Y4M8zQxqfQ9Rz5K', 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (user_id, display_name, business_name, avatar_url, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Default Buyer', NULL, NULL, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'Default Seller', 'EscrowX Seller', NULL, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'Default Admin', NULL, NULL, NOW(), NOW()),
    ('44444444-4444-4444-4444-444444444444', 'Default Super Admin', NULL, NULL, NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;
