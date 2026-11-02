package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.model.Route;

@ApplicationScoped
public class RouteRepository {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "r.id", "name", "r.name", "creationDate", "r.creationDate",
            "distance", "r.distance", "rating", "r.rating", "from", "r.from.id", "to", "r.to.id");

    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    public record Page(List<Route> items, long total, int page, int size) {
    }

    public static boolean supportsSort(String field) {
        return SORT_COLUMNS.containsKey(field);
    }

    public Page findPage(int page, int size, String filter, String sortBy, String direction) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        String orderField = sortBy == null ? "r.id" : SORT_COLUMNS.getOrDefault(sortBy, "r.id");
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
        List<Route> items = query
                .setFirstResult(currentPage * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return new Page(items, total, currentPage, pageSize);
    }

    public Route findById(long id) {
        return entityManager.find(Route.class, id);
    }

    public Route findByIdForUpdate(long id) {
        return entityManager.find(Route.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public void persist(Route route) {
        entityManager.persist(route);
        entityManager.flush();
        entityManager.refresh(route);
    }

    public void flush() {
        entityManager.flush();
    }

    public void delete(Route route) {
        entityManager.remove(route);
    }

    public long countByLocation(Location location) {
        return entityManager.createQuery(
                "select count(r) from Route r where r.from = :location or r.to = :location", Long.class)
                .setParameter("location", location)
                .getSingleResult();
    }

    public void replaceLocation(Location location, Location replacement) {
        entityManager.createQuery("update Route r set r.from = :replacement where r.from = :location")
                .setParameter("replacement", replacement)
                .setParameter("location", location)
                .executeUpdate();
        entityManager.createQuery("update Route r set r.to = :replacement where r.to = :location")
                .setParameter("replacement", replacement)
                .setParameter("location", location)
                .executeUpdate();
    }

    public double getAverageRating() {
        Number result = (Number) entityManager.createNativeQuery("select {h-schema}get_avg_rating()").getSingleResult();
        return result.doubleValue();
    }

    public long countRoutesBelowRating(long rating) {
        Number result = (Number) entityManager.createNativeQuery("select {h-schema}count_routes_below_rating(:rating)")
                .setParameter("rating", rating)
                .getSingleResult();
        return result.longValue();
    }

    public List<Long> getUniqueRatings() {
        Object result = entityManager.createNativeQuery("select {h-schema}get_unique_ratings()").getSingleResult();
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

    public Route findShortestRoute(long fromLocationId, long toLocationId) {
        List<Route> routes = entityManager.createNativeQuery("select * from {h-schema}get_shortest_route(:fromId, :toId)", Route.class)
                .setParameter("fromId", fromLocationId)
                .setParameter("toId", toLocationId)
                .getResultList();
        if (routes.isEmpty()) {
            return null;
        }
        return routes.get(0);
    }

    public long addBetweenLocations(String name, long coordinatesId, Long fromId, long toId, double distance, long rating) {
        Number id = (Number) entityManager.createNativeQuery("select {h-schema}add_route_between_locations(:name, :coordsId, :fromId, :toId, :distance, :rating)")
                .setParameter("name", name)
                .setParameter("coordsId", coordinatesId)
                .setParameter("fromId", fromId)
                .setParameter("toId", toId)
                .setParameter("distance", distance)
                .setParameter("rating", rating)
                .getSingleResult();
        return id.longValue();
    }
}
