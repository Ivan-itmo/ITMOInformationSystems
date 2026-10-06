package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.RouteValidator;

@ApplicationScoped
public class RouteOperationsService {
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;
    @Inject
    private RouteValidator validator;
    @Inject
    private RouteEntityManager entities;
    @Inject
    private RouteTransactionNotifier notifications;

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
        validator.validateRouteBetweenLocations(request);
        if (request.fromLocationId() != null) {
            entities.findLocationEntity(request.fromLocationId());
        }
        entities.findLocationEntity(request.toLocationId());
        Coordinates coordinates = entities.createCoordinates(request.coordinates());
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
        notifications.routesChangedAfterCommit();
        return Mapper.toDto(entities.findRouteEntity(id.longValue()));
    }
}
