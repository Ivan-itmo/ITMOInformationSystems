package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.itmo.routes.repository.RouteRepository;
import ru.itmo.routes.repository.LocationRepository;
import ru.itmo.routes.dto.CoordinatesRequest;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.validation.NotFoundException;

@ApplicationScoped
public class RouteEntityResolver {
    @Inject
    private RouteRepository routes;
    @Inject
    private LocationRepository locations;

    public Route findRouteEntity(long id) {
        Route route = routes.findById(id);
        if (route == null) {
            throw new NotFoundException("Маршрут с id " + id + " не найден");
        }
        return route;
    }

    public Location findLocationEntity(long id) {
        Location location = locations.findById(id);
        if (location == null) {
            throw new NotFoundException("Локация с id " + id + " не найден");
        }
        return location;
    }

    public Route findRouteEntityForUpdate(long id) {
        Route route = routes.findByIdForUpdate(id);
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
