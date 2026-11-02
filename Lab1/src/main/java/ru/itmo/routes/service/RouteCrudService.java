package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Route;
import ru.itmo.routes.repository.RouteRepository;
import ru.itmo.routes.validation.RouteValidator;

@ApplicationScoped
@Transactional
public class RouteCrudService {
    @Inject
    private RouteRepository repository;
    @Inject
    private RouteEntityResolver entities;
    @Inject
    private RouteValidator validator;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        validator.validateRouteQuery(page, size, sortBy, direction);
        RouteRepository.Page result = repository.findPage(page, size, filter, sortBy, direction);
        return new PageResponse<>(result.items().stream().map(Mapper::toDto).toList(), result.total(), result.page(), result.size());
    }

    public RouteResponse findRoute(long id) {
        return Mapper.toDto(entities.findRouteEntity(id));
    }

    public RouteResponse createRoute(RouteRequest request) {
        validator.validateRoute(request);
        Route route = new Route();
        applyRouteFields(route, request);
        repository.persist(route);
        return Mapper.toDto(route);
    }

    public RouteResponse updateRoute(long id, RouteRequest request) {
        validator.validateRoute(request);
        Route route = entities.findRouteEntityForUpdate(id);
        validator.validateRouteUpdate(request, Mapper.toDto(route));
        applyRouteFields(route, request);
        repository.flush();
        return Mapper.toDto(route);
    }

    public void deleteRoute(long id) {
        repository.delete(entities.findRouteEntityForUpdate(id));
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
