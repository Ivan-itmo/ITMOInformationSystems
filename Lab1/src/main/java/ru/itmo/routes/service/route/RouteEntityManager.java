package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import ru.itmo.routes.dto.CoordinatesRequest;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.service.NotFoundException;

@ApplicationScoped
public class RouteEntityManager {
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    public Route findRouteEntity(long id) {
        Route route = entityManager.find(Route.class, id);
        if (route == null) {
            throw new NotFoundException("Маршрут с id " + id + " не найден");
        }
        return route;
    }

    public Location findLocationEntity(long id) {
        Location location = entityManager.find(Location.class, id);
        if (location == null) {
            throw new NotFoundException("Локация с id " + id + " не найдена");
        }
        return location;
    }

    public Route findRouteEntityForUpdate(long id) {
        Route route = entityManager.find(Route.class, id);
        if (route == null) {
            throw new NotFoundException("Маршрут с id " + id + " удалён");
        }
        return route;
    }

    public Coordinates createCoordinates(CoordinatesRequest dto) {
        Coordinates coordinates = new Coordinates();
        coordinates.setX(dto.x());
        coordinates.setY(dto.y());
        return coordinates;
    }
}
