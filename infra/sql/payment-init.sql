-- Payment Service Schema
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id VARCHAR(100) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    stripe_client_secret VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_user ON payments(user_email);
