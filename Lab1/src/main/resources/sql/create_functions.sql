SET search_path TO s465544;

-- 1. Рассчитать среднее значение поля rating
CREATE OR REPLACE FUNCTION get_avg_rating()
RETURNS DOUBLE PRECISION AS $$
BEGIN
RETURN COALESCE((SELECT AVG(rating) FROM route), 0.0);
END;
$$ LANGUAGE plpgsql;

-- 2. Вернуть количество объектов, значение поля rating которых меньше заданного
CREATE OR REPLACE FUNCTION count_routes_below_rating(p_rating BIGINT)
RETURNS BIGINT AS $$
BEGIN
RETURN (SELECT COUNT(*) FROM route WHERE rating < p_rating);
END;
$$ LANGUAGE plpgsql;

-- 3. Вернуть массив уникальных значений поля rating по всем объектам
CREATE OR REPLACE FUNCTION get_unique_ratings()
RETURNS BIGINT[] AS $$
BEGIN
RETURN (SELECT COALESCE(array_agg(DISTINCT rating), ARRAY[]::BIGINT[]) FROM route);
END;
$$ LANGUAGE plpgsql;

-- 4. Найти самый короткий маршрут между указанными локациями
CREATE OR REPLACE FUNCTION get_shortest_route(p_from_id BIGINT, p_to_id BIGINT)
RETURNS SETOF route AS $$
BEGIN
RETURN QUERY
SELECT * FROM route
WHERE from_location_id = p_from_id AND to_location_id = p_to_id
ORDER BY distance ASC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- 5. Добавить новый маршрут между указанными локациями
CREATE OR REPLACE FUNCTION add_route_between_locations(
    p_name VARCHAR,
    p_coords_id BIGINT,
    p_from_id BIGINT,
    p_to_id BIGINT,
    p_distance DOUBLE PRECISION,
    p_rating BIGINT
) RETURNS BIGINT AS $$
DECLARE
new_id BIGINT;
BEGIN
INSERT INTO route (name, coordinates_id, from_location_id, to_location_id, distance, rating)
VALUES (p_name, p_coords_id, p_from_id, p_to_id, p_distance, p_rating)
    RETURNING id INTO new_id;
RETURN new_id;
END;
$$ LANGUAGE plpgsql;
