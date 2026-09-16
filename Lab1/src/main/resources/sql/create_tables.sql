-- Координаты
CREATE TABLE IF NOT EXISTS coordinates (
                                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                           x DOUBLE PRECISION NOT NULL,
                                           y BIGINT NOT NULL
);

-- Локация
CREATE TABLE IF NOT EXISTS location (
                                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        x INTEGER NOT NULL,
                                        y BIGINT NOT NULL,
                                        z REAL NOT NULL
);

-- Маршрут
CREATE TABLE IF NOT EXISTS route (
                                     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    coordinates_id BIGINT NOT NULL REFERENCES coordinates(id) ON DELETE CASCADE,
    from_location_id BIGINT REFERENCES location(id) ON DELETE SET NULL,
    to_location_id BIGINT NOT NULL REFERENCES location(id) ON DELETE RESTRICT,
    distance DOUBLE PRECISION NOT NULL CHECK (distance > 1),
    rating BIGINT NOT NULL CHECK (rating > 0)
    );