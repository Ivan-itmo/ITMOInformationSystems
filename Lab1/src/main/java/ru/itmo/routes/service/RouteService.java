package ru.itmo.routes.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.CoordinatesDto;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationDto;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.model.Route;

@ApplicationScoped
public class RouteService {
    private static final List<String> sort_columns = List.of("id", "name", "creationDate", "distance", "rating");

    @Inject
    private RouteChangeNotifier changeNotifier;

    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String orderField = sort_columns.contains(sortBy) ? sortBy : "id";
        String orderDirection = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase();

        String where = normalizedFilter.isBlank() ? "" : " where lower(r.name) like :filter";
        TypedQuery<Route> query = entityManager.createQuery("select r from Route r" + where + " order by r." + orderField + " " + orderDirection, Route.class);
        TypedQuery<Long> countQuery = entityManager.createQuery("select count(r) from Route r" + where, Long.class);
        if (!normalizedFilter.isBlank()) {
            String pattern = "%" + normalizedFilter + "%";
            query.setParameter("filter", pattern);
            countQuery.setParameter("filter", pattern);
        }

        List<RouteResponse> items = query
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultStream()
                .map(Mapper::toDto)
                .toList();

        return new PageResponse<>(items, countQuery.getSingleResult(), safePage, safeSize);
    }

    public RouteResponse findRoute(long id) {
        return Mapper.toDto(findRouteEntity(id));
    }

    public List<LocationDto> findLocations() {
        return entityManager.createQuery("select l from Location l order by l.id", Location.class)
                .getResultStream()
                .map(Mapper::toDto)
                .toList();
    }

    @Transactional
    public LocationDto createLocation(LocationRequest request) {
        validateLocation(request);
        Location location = new Location();
        location.setX(request.x());
        location.setY(request.y());
        location.setZ(request.z());
        entityManager.persist(location);
        notifyAfterCommit(() -> {
            changeNotifier.locationsChanged();
            changeNotifier.routesChanged();
        });
        return Mapper.toDto(location);
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        validateRoute(request);
        Route route = new Route();
        applyRouteFields(route, request);
        entityManager.persist(route);
        entityManager.flush();
        entityManager.refresh(route);
        notifyRoutesAfterCommit();
        return Mapper.toDto(route);
    }

    @Transactional
    public RouteResponse updateRoute(long id, RouteRequest request) {
        validateRoute(request);
        Route route = findRouteEntity(id);
        applyRouteFields(route, request);
        entityManager.flush();
        notifyRoutesAfterCommit();
        return Mapper.toDto(route);
    }

    @Transactional
    public void deleteRoute(long id) {
        entityManager.remove(findRouteEntity(id));
        notifyRoutesAfterCommit();
    }

    public long countLocationRoutes(long id) {
        return countLocationRoutes(findLocationEntity(id));
    }

    private long countLocationRoutes(Location location) {
        return entityManager.createQuery(
                "select count(r) from Route r where r.from = :location or r.to = :location", Long.class)
                .setParameter("location", location)
                .getSingleResult();
    }

    @Transactional
    public void deleteLocation(long id, DeleteLocationRequest request) {
        // Block concurrent foreign-key references until reassignment and deletion commit.
        List<?> lockedIds = entityManager.createNativeQuery(
                "select id from {h-schema}location where id = :id for update")
                .setParameter("id", id)
                .getResultList();
        if (lockedIds.isEmpty()) {
            throw new NotFoundException("Локация с id " + id + " не найдена");
        }
        Location location = findLocationEntity(id);
        Long replacementId = request == null ? null : request.replacementLocationId();
        if (replacementId != null && replacementId == id) {
            throw new ValidationException("Выберите другую локацию для замены");
        }
        Location replacement = replacementId == null ? null : findLocationEntity(replacementId);
        if (countLocationRoutes(location) > 0 && replacement == null) {
            throw new ValidationException("Локация используется маршрутами. Выберите локацию для замены");
        }
        if (replacement != null) {
            entityManager.createQuery("update Route r set r.from = :replacement where r.from = :location")
                    .setParameter("replacement", replacement)
                    .setParameter("location", location)
                    .executeUpdate();
            entityManager.createQuery("update Route r set r.to = :replacement where r.to = :location")
                    .setParameter("replacement", replacement)
                    .setParameter("location", location)
                    .executeUpdate();
        }
        entityManager.remove(location);
        entityManager.flush();
        notifyAfterCommit(() -> {
            changeNotifier.locationsChanged();
            changeNotifier.routesChanged();
        });
    }

    public double getAverageRating() {
        Number result = (Number) entityManager.createNativeQuery("select get_avg_rating()").getSingleResult();
        return result.doubleValue();
    }

    public long countRoutesBelowRating(long rating) {
        Number result = (Number) entityManager.createNativeQuery("select count_routes_below_rating(:rating)")
                .setParameter("rating", rating)
                .getSingleResult();
        return result.longValue();
    }

    public List<Long> getUniqueRatings() {
        Object result = entityManager.createNativeQuery("select get_unique_ratings()").getSingleResult();
        try {
            Object array = result instanceof Array sqlArray ? sqlArray.getArray() : result;
            if (array instanceof Long[] ratings) {
                return Arrays.asList(ratings);
            }
            if (array instanceof Object[] ratings) {
                return Arrays.stream(ratings).map(value -> ((Number) value).longValue()).toList();
            }
            return List.of();
        } catch (SQLException exception) {
            throw new IllegalStateException("Не удалось прочитать массив рейтингов из БД", exception);
        }
    }

    public RouteResponse getShortestRoute(long fromLocationId, long toLocationId) {
        List<Route> routes = entityManager.createNativeQuery("select * from get_shortest_route(:fromId, :toId)", Route.class)
                .setParameter("fromId", fromLocationId)
                .setParameter("toId", toLocationId)
                .getResultList();
        if (routes.isEmpty()) {
            throw new NotFoundException("Маршрут между выбранными локациями не найден");
        }
        return Mapper.toDto(routes.get(0));
    }

    @Transactional
    public RouteResponse addRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        validateRoute(new RouteRequest(
                request.name(),
                request.coordinates(),
                request.fromLocationId(),
                request.toLocationId(),
                request.distance(),
                request.rating()
        ));
        Coordinates coordinates = createCoordinates(request.coordinates());
        entityManager.persist(coordinates);
        entityManager.flush();
        Number id = (Number) entityManager.createNativeQuery("select add_route_between_locations(:name, :coordsId, :fromId, :toId, :distance, :rating)")
                .setParameter("name", request.name().trim())
                .setParameter("coordsId", coordinates.getId())
                .setParameter("fromId", request.fromLocationId())
                .setParameter("toId", request.toLocationId())
                .setParameter("distance", request.distance())
                .setParameter("rating", request.rating())
                .getSingleResult();
        entityManager.flush();
        notifyRoutesAfterCommit();
        return findRoute(id.longValue());
    }

    private void applyRouteFields(Route route, RouteRequest request) {
        route.setName(request.name().trim());
        route.setDistance(request.distance());
        route.setRating(request.rating());
        route.setFrom(request.fromLocationId() == null ? null : findLocationEntity(request.fromLocationId()));
        route.setTo(findLocationEntity(request.toLocationId()));

        Coordinates coordinates = route.getCoordinates();
        if (coordinates == null) {
            coordinates = createCoordinates(request.coordinates());
            route.setCoordinates(coordinates);
        } else {
            coordinates.setX(request.coordinates().x());
            coordinates.setY(request.coordinates().y());
        }
    }

    private Coordinates createCoordinates(CoordinatesDto dto) {
        Coordinates coordinates = new Coordinates();
        coordinates.setX(dto.x());
        coordinates.setY(dto.y());
        return coordinates;
    }

    private Route findRouteEntity(long id) {
        Route route = entityManager.find(Route.class, id);
        if (route == null) {
            throw new NotFoundException("Маршрут с id " + id + " не найден");
        }
        return route;
    }

    private Location findLocationEntity(long id) {
        Location location = entityManager.find(Location.class, id);
        if (location == null) {
            throw new NotFoundException("Локация с id " + id + " не найдена");
        }
        return location;
    }

    private void validateRoute(RouteRequest request) {
        if (request == null) {
            throw new ValidationException("Данные маршрута обязательны");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ValidationException("Название маршрута не может быть пустым");
        }
        if (request.coordinates() == null || !isFinite(request.coordinates().x()) || !isFinite(request.coordinates().y())) {
            throw new ValidationException("Координаты маршрута обязательны");
        }
        if (request.toLocationId() == null) {
            throw new ValidationException("Локация назначения обязательна");
        }
        if (request.distance() == null || !Double.isFinite(request.distance()) || request.distance() <= 0) {
            throw new ValidationException("Дистанция должна быть больше 0");
        }
        if (request.rating() == null || request.rating() <= 0) {
            throw new ValidationException("Рейтинг должен быть больше 0");
        }
    }

    private void validateLocation(LocationRequest request) {
        if (request == null || !isFinite(request.x()) || !isFinite(request.y()) || !isFinite(request.z())) {
            throw new ValidationException("Координаты локации должны быть конечными числами");
        }
    }

    private boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private void notifyRoutesAfterCommit() {
        notifyAfterCommit(changeNotifier::routesChanged);
    }

    private void notifyAfterCommit(Runnable action) {
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    action.run();
                }
            }
        });
    }
}
