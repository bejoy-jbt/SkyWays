-- Run this manually after first startup to create an admin user
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (email, password_hash, first_name, last_name, role, enabled)
VALUES (
    'admin@flightapp.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin',
    'User',
    'ADMIN',
    true
) ON CONFLICT (email) DO NOTHING;
