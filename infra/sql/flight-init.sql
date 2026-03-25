-- Flight Service Schema
CREATE TABLE IF NOT EXISTS flights (
    id BIGSERIAL PRIMARY KEY,
    flight_number VARCHAR(20) UNIQUE NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_date DATE NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    aircraft_type VARCHAR(100),
    total_rows INT NOT NULL DEFAULT 28,
    seats_per_row INT NOT NULL DEFAULT 6,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    flight_id BIGINT NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_by_booking_id VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(flight_id, seat_number)
);

CREATE INDEX idx_seats_flight_status ON seats(flight_id, status);
CREATE INDEX idx_flights_route_date ON flights(origin, destination, departure_date);
