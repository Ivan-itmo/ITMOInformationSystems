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
import ru.itmo.routes.service.ConflictException;
import ru.itmo.routes.service.ValidationException;
import ru.itmo.routes.service.RouteValidator;

@ApplicationScoped
public class RouteCrudService {
    private static final Map<String, String> sortColomns = Map.of("id", "r.id", "name", "r.name", "creationDate", "r.creationDate", "distance", "r.distance", "rating", "r.rating", "from", "r.from.id", "to", "r.to.id");
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;
    @Inject
    private RouteEntityManager entities;
    @Inject
    private RouteValidator validator;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        if (page < 0 || size < 1) {
            throw new ValidationException("Номер страницы должен быть неотрицательным, размер - больше 0");
        }
        if (sortBy != null && !sortColomns.containsKey(sortBy)) {
            throw new ValidationException("Неизвестное поле сортировки");
        }
        if (direction != null && !"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new ValidationException("Направление сортировки должно быть asc или desc");
        }
        int pageSize = Math.min(Math.max(size, 1), 100);
        String orderField = sortBy == null ? "r.id" : sortColomns.getOrDefault(sortBy, "r.id");
        String orderDirection = "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase();

        String where = normalizedFilter.isEmpty() ? "" : " where lower(r.name) like :filter";
        TypedQuery<Route> query = entityManager.createQuery("select r from Route r join fetch r.coordinates left join fetch r.from join fetch r.to" + where + " order by " + orderField + " " + orderDirection + ", r.id asc", Route.class);
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
                .getResultList()
                .stream()
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
        return Mapper.toDto(route);
    }

    @Transactional
    public RouteResponse updateRoute(long id, RouteRequest request) {
        validator.validateRoute(request);
        Route route = entities.findRouteEntityForUpdate(id);
        if (request.expectedState() == null) {
            throw new ValidationException("Перед сохранением необходимо загрузить исходное состояние маршрута");
        }
        if (!Mapper.toDto(route).equals(request.expectedState())) {
            throw new ConflictException("Объект изменён другим пользователем. Откройте его заново.");
        }
        applyRouteFields(route, request);
        entityManager.flush();
        return Mapper.toDto(route);
    }

    @Transactional
    public void deleteRoute(long id) {
        entityManager.remove(entities.findRouteEntityForUpdate(id));
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
