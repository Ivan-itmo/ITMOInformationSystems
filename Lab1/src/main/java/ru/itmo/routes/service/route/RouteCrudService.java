package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.service.RouteValidator;

@ApplicationScoped
public class RouteCrudService {
    private static final Map<String, String> sortColomns = Map.of("id", "r.id", "name", "r.name", "creationDate", "r.creationDate", "distance", "r.distance", "rating", "r.rating", "from", "origin.id", "to", "destination.id");
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;
    @Inject
    private RouteValidator validator;
    @Inject
    private RouteEntityManager entities;
    @Inject
    private RouteTransactionNotifier notifications;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        String orderField = sortBy == null ? "r.id" : sortColomns.getOrDefault(sortBy, "r.id");
        String orderDirection = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase();

        String where = normalizedFilter.isEmpty() ? "" : " where lower(r.name) like :filter";
        TypedQuery<Route> query = entityManager.createQuery("select r from Route r left join r.from origin left join r.to destination" + where + " order by " + orderField + " " + orderDirection + ", r.id asc", Route.class);
        TypedQuery<Long> countQuery = entityManager.createQuery("select count(r) from Route r" + where, Long.class);
        if (!normalizedFilter.isBlank()) {
            String pattern = "%" + normalizedFilter + "%";
            query.setParameter("filter", pattern);
            countQuery.setParameter("filter", pattern);
        }

        long total = countQuery.getSingleResult();
        int lastPage = (int) Math.max(0, (total - 1) / pageSize);
        int currentPage = Math.min(Math.max(page, 0), lastPage);
        List<RouteResponse> items = query
                .setFirstResult(currentPage * pageSize)
                .setMaxResults(pageSize)
                .getResultStream()
                .map(Mapper::toDto)
                .toList();
        return new PageResponse<>(items, total, currentPage, pageSize);
    }

    public RouteResponse findRoute(long id) {
        return Mapper.toDto(entities.findRouteEntity(id));
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        validator.validateRoute(request);
        Route route = new Route();
        applyRouteFields(route, request);
        entityManager.persist(route);
        entityManager.flush();
        entityManager.refresh(route);
        notifications.routesChangedAfterCommit();
        return Mapper.toDto(route);
    }

    @Transactional
    public RouteResponse updateRoute(long id, RouteRequest request) {
        validator.validateRoute(request);
        Route route = entities.findRouteEntity(id);
        applyRouteFields(route, request);
        entityManager.flush();
        notifications.routesChangedAfterCommit();
        return Mapper.toDto(route);
    }

    @Transactional
    public void deleteRoute(long id) {
        entityManager.remove(entities.findRouteEntity(id));
        notifications.routesChangedAfterCommit();
    }

    private void applyRouteFields(Route route, RouteRequest request) {
        route.setName(request.name().trim());
        route.setDistance(request.distance());
        route.setRating(request.rating());
        route.setFrom(request.fromLocationId() == null ? null : entities.findLocationEntity(request.fromLocationId()));
        route.setTo(entities.findLocationEntity(request.toLocationId()));

        Coordinates coordinates = route.getCoordinates();
        if (coordinates == null) {
            coordinates = entities.createCoordinates(request.coordinates());
            route.setCoordinates(coordinates);
        } else {
            coordinates.setX(request.coordinates().x());
            coordinates.setY(request.coordinates().y());
        }
    }
}
