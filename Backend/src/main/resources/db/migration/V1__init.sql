-- ==========================================
-- ENUMY
-- ==========================================

CREATE TYPE user_role AS ENUM ('passenger', 'inspector', 'admin');

CREATE TYPE import_status AS ENUM ('in_progress', 'success', 'failed');

CREATE TYPE transaction_type AS ENUM ('top_up', 'ticket_purchase', 'refund', 'penalty_payment');

CREATE TYPE penalty_status AS ENUM ('unpaid', 'paid', 'appealed', 'cancelled');

-- ==========================================
-- SLOWNIKI
-- ==========================================

CREATE TABLE vehicle_types (
    code            TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    gtfs_route_type INTEGER UNIQUE
);

INSERT INTO vehicle_types (code, name, gtfs_route_type) VALUES
    ('tram',  'Tramwaj', 0),
    ('metro', 'Metro',   1),
    ('bus',   'Autobus', 3);

-- ==========================================
-- 1. ROZKLADY JAZDY (GTFS) I POZYCJE GPS
-- ==========================================

CREATE TABLE gtfs_imports (
    import_id     BIGSERIAL PRIMARY KEY,
    fetched_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    checksum      TEXT NOT NULL,
    status        import_status NOT NULL DEFAULT 'in_progress',
    error_message TEXT
);

CREATE TABLE routes (
    route_id          TEXT PRIMARY KEY,
    short_name        TEXT NOT NULL,
    long_name         TEXT,
    vehicle_type_code TEXT NOT NULL REFERENCES vehicle_types (code) ON DELETE RESTRICT,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    last_import_id    BIGINT NOT NULL REFERENCES gtfs_imports (import_id)
);

CREATE TABLE services (
    service_id  TEXT PRIMARY KEY,
    description TEXT
);

CREATE TABLE service_dates (
    service_id   TEXT NOT NULL REFERENCES services (service_id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    PRIMARY KEY (service_id, service_date)
);

CREATE INDEX idx_service_dates_service_date ON service_dates (service_date);

CREATE TABLE trips (
    trip_id        TEXT PRIMARY KEY,
    route_id       TEXT NOT NULL REFERENCES routes (route_id),
    service_id     TEXT NOT NULL REFERENCES services (service_id) ON DELETE RESTRICT,
    direction_id   SMALLINT NOT NULL DEFAULT 0,
    trip_headsign  TEXT,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    last_import_id BIGINT NOT NULL REFERENCES gtfs_imports (import_id)
);

CREATE INDEX idx_trips_route_service_active ON trips (route_id, service_id, is_active);

CREATE TABLE stops (
    stop_id        TEXT PRIMARY KEY,
    parent_station TEXT REFERENCES stops (stop_id) ON DELETE SET NULL,
    stop_name      TEXT NOT NULL,
    stop_lat       DOUBLE PRECISION NOT NULL,
    stop_lon       DOUBLE PRECISION NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    last_import_id BIGINT NOT NULL REFERENCES gtfs_imports (import_id)
);

CREATE TABLE stop_times (
    trip_id        TEXT NOT NULL REFERENCES trips (trip_id) ON DELETE CASCADE,
    stop_sequence  INTEGER NOT NULL,
    stop_id        TEXT NOT NULL REFERENCES stops (stop_id) ON DELETE RESTRICT,
    arrival_time   INTERVAL,
    departure_time INTERVAL,
    PRIMARY KEY (trip_id, stop_sequence)
);

CREATE INDEX idx_stop_times_stop_id ON stop_times (stop_id);

CREATE TABLE route_shapes (
    route_id       TEXT NOT NULL REFERENCES routes (route_id) ON DELETE CASCADE,
    direction_id   SMALLINT NOT NULL,
    points_json    TEXT NOT NULL,
    last_import_id BIGINT REFERENCES gtfs_imports (import_id),
    PRIMARY KEY (route_id, direction_id)
);

CREATE TABLE route_stops (
    route_id      TEXT NOT NULL REFERENCES routes (route_id) ON DELETE CASCADE,
    stop_id       TEXT NOT NULL REFERENCES stops (stop_id) ON DELETE CASCADE,
    direction_id  SMALLINT NOT NULL,
    stop_sequence INTEGER,
    headsign      TEXT,
    PRIMARY KEY (route_id, stop_id, direction_id)
);

CREATE INDEX idx_route_stops_stop_id ON route_stops (stop_id);
CREATE INDEX idx_route_stops_route_direction ON route_stops (route_id, direction_id, stop_sequence);

CREATE TABLE active_vehicles (
    mpk_course_id     BIGINT PRIMARY KEY,
    line              TEXT NOT NULL,
    vehicle_type_code TEXT NOT NULL REFERENCES vehicle_types (code) ON DELETE RESTRICT,
    lat               DOUBLE PRECISION NOT NULL,
    lon               DOUBLE PRECISION NOT NULL,
    route_id          TEXT REFERENCES routes (route_id) ON DELETE SET NULL,
    trip_id           TEXT REFERENCES trips (trip_id) ON DELETE SET NULL,
    next_stop_id      TEXT REFERENCES stops (stop_id) ON DELETE SET NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_active_vehicles_line ON active_vehicles (line);
CREATE INDEX idx_active_vehicles_trip_id ON active_vehicles (trip_id);
CREATE INDEX idx_active_vehicles_next_stop_id ON active_vehicles (next_stop_id);
CREATE INDEX idx_active_vehicles_updated_at ON active_vehicles (updated_at);

CREATE TABLE vehicle_positions (
    position_id       BIGSERIAL PRIMARY KEY,
    mpk_course_id     BIGINT NOT NULL,
    line              TEXT NOT NULL,
    vehicle_type_code TEXT NOT NULL REFERENCES vehicle_types (code) ON DELETE RESTRICT,
    lat               DOUBLE PRECISION NOT NULL,
    lon               DOUBLE PRECISION NOT NULL,
    route_id          TEXT REFERENCES routes (route_id) ON DELETE SET NULL,
    trip_id           TEXT REFERENCES trips (trip_id) ON DELETE SET NULL,
    fetched_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_vehicle_positions_course_fetched ON vehicle_positions (mpk_course_id, fetched_at);
CREATE INDEX idx_vehicle_positions_line_fetched ON vehicle_positions (line, fetched_at);

-- ==========================================
-- 2. UZYTKOWNICY I PERSONALIZACJA
-- ==========================================

CREATE TABLE users (
    user_id        BIGSERIAL PRIMARY KEY,
    email          TEXT NOT NULL UNIQUE,
    password_hash  TEXT NOT NULL,
    role           user_role NOT NULL DEFAULT 'passenger',
    wallet_balance NUMERIC(10, 2) NOT NULL DEFAULT 0
);

CREATE TABLE favorite_stops (
    user_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    stop_id TEXT NOT NULL REFERENCES stops (stop_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, stop_id)
);

CREATE TABLE search_history (
    search_id   BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    query       TEXT NOT NULL,
    searched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_search_history_user_searched ON search_history (user_id, searched_at);

-- ==========================================
-- 3. BILETY I PORTFEL
-- ==========================================

CREATE TABLE ticket_types (
    ticket_type_id   BIGSERIAL PRIMARY KEY,
    name             TEXT NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    validity_minutes INTEGER NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE purchased_tickets (
    ticket_id                  BIGSERIAL PRIMARY KEY,
    user_id                    BIGINT NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT,
    ticket_type_id             BIGINT NOT NULL REFERENCES ticket_types (ticket_type_id) ON DELETE RESTRICT,
    validated_route_id         TEXT REFERENCES routes (route_id) ON DELETE SET NULL,
    validated_route_short_name TEXT,
    code                       TEXT NOT NULL UNIQUE,
    price_paid                 NUMERIC(10, 2) NOT NULL,
    purchased_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    validated_at               TIMESTAMPTZ,
    expires_at                 TIMESTAMPTZ
);

CREATE INDEX idx_purchased_tickets_user_id ON purchased_tickets (user_id);
CREATE INDEX idx_purchased_tickets_expires_at ON purchased_tickets (expires_at);

-- ==========================================
-- 4. KONTROLE BILETOW I MANDATY
-- ==========================================

CREATE TABLE inspections (
    inspection_id    BIGSERIAL PRIMARY KEY,
    inspector_id     BIGINT NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT,
    ticket_id        BIGINT REFERENCES purchased_tickets (ticket_id) ON DELETE SET NULL,
    route_id         TEXT REFERENCES routes (route_id) ON DELETE SET NULL,
    route_short_name TEXT,
    inspected_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_valid         BOOLEAN NOT NULL
);

CREATE INDEX idx_inspections_inspector_at ON inspections (inspector_id, inspected_at);
CREATE INDEX idx_inspections_ticket_id ON inspections (ticket_id);

CREATE TABLE penalties (
    penalty_id           BIGSERIAL PRIMARY KEY,
    inspection_id        BIGINT NOT NULL UNIQUE REFERENCES inspections (inspection_id) ON DELETE RESTRICT,
    user_id              BIGINT REFERENCES users (user_id) ON DELETE SET NULL,
    penalty_number       TEXT NOT NULL UNIQUE,
    offender_full_name   TEXT,
    offender_document_id TEXT,
    amount               NUMERIC(10, 2) NOT NULL,
    status               penalty_status NOT NULL DEFAULT 'unpaid',
    due_date             TIMESTAMPTZ NOT NULL,
    issued_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_penalties_user_id ON penalties (user_id);
CREATE INDEX idx_penalties_status_due ON penalties (status, due_date);

CREATE TABLE wallet_transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT,
    type           transaction_type NOT NULL,
    ticket_id      BIGINT REFERENCES purchased_tickets (ticket_id) ON DELETE RESTRICT,
    penalty_id     BIGINT REFERENCES penalties (penalty_id) ON DELETE RESTRICT,
    amount         NUMERIC(10, 2) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallet_transactions_user_created ON wallet_transactions (user_id, created_at);
