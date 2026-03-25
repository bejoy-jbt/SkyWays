-- Booking Service Schema
CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(100) PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    flight_id BIGINT NOT NULL,
    flight_number VARCHAR(20) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_intent_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    confirmed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS passengers (
    id BIGSERIAL PRIMARY KEY,
    booking_id VARCHAR(100) REFERENCES bookings(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth VARCHAR(20) NOT NULL,
    passport_number VARCHAR(50),
    nationality VARCHAR(100),
    special_requests TEXT
);

CREATE INDEX idx_bookings_user ON bookings(user_email);
CREATE INDEX idx_bookings_flight_seat ON bookings(flight_id, seat_number);
