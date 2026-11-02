package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.repository.RouteRepository;
import ru.itmo.routes.repository.CoordinatesRepository;
import ru.itmo.routes.validation.NotFoundException;
import ru.itmo.routes.validation.RouteValidator;

@ApplicationScoped
@Transactional
public class RouteOperationsService {
    @Inject
    private RouteRepository routes;
    @Inject
    private CoordinatesRepository coordinatesRepository;
    @Inject
    private RouteEntityResolver entities;
    @Inject
    private RouteValidator validator;

    public double getAverageRating() {
        return routes.getAverageRating();
    }

    public long countRoutesBelowRating(long rating) {
        return routes.countRoutesBelowRating(rating);
    }

    public List<Long> getUniqueRatings() {
        return routes.getUniqueRatings();
    }

    public RouteResponse getShortestRoute(long fromLocationId, long toLocationId) {
        Route route = routes.findShortestRoute(fromLocationId, toLocationId);
        if (route == null) {
            throw new NotFoundException("Маршрут между выбранными локациями не найден");
        }
        return Mapper.toDto(route);
    }

    public RouteResponse addRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        validator.validateRouteBetweenLocations(request);
        if (request.fromLocationId() != null) {
            entities.findLocationEntity(request.fromLocationId());
        }
        entities.findLocationEntity(request.toLocationId());
        Coordinates coordinates = entities.createCoordinates(request.coordinates());
        coordinatesRepository.persist(coordinates);
        long id = routes.addBetweenLocations(request.name().trim(), coordinates.getId(),
                request.fromLocationId(), request.toLocationId(), request.distance(), request.rating());
        return Mapper.toDto(entities.findRouteEntity(id));
    }
}
