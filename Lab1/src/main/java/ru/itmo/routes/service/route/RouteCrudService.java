package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.util.List;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.service.RouteValidator;

@ApplicationScoped
public class RouteCrudService {
    private List<String> sortColomns = List.of("id", "name", "creationDate", "distance", "rating");

    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    @Inject
    private RouteValidator validator;

    @Inject
    private RouteEntityManager entities;

    @Inject
    private RouteTransactionNotifier notifications;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String orderField = sortColomns.contains(sortBy) ? sortBy : "id";
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
