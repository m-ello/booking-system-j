CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE resources (
                           id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           name        VARCHAR(100) NOT NULL,
                           description VARCHAR(500),
                           capacity    INTEGER      NOT NULL,
                           active      BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           CONSTRAINT chk_resources_capacity CHECK (capacity > 0)
);

CREATE TABLE bookings (
                          id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id     BIGINT      NOT NULL REFERENCES users (id),
                          resource_id BIGINT      NOT NULL REFERENCES resources (id),
                          start_time  TIMESTAMPTZ NOT NULL,
                          end_time    TIMESTAMPTZ NOT NULL,
                          status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
                          created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                          CONSTRAINT chk_bookings_time   CHECK (end_time > start_time),
                          CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_bookings_user ON bookings (user_id);
CREATE INDEX idx_bookings_resource_time ON bookings (resource_id, start_time, end_time);