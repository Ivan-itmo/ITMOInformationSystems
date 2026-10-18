package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationDto;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.service.RouteChangeNotifier;

@ApplicationScoped
public class RouteManager {
    @Inject
    private RouteCrudService routes;

    @Inject
    private LocationService locations;

    @Inject
    private RouteOperationsService operations;

    @Inject
    private RouteChangeNotifier notifications;

    public PageResponse<RouteResponse> findRoutes(int page, int size, String filter, String sortBy, String direction) {
        return routes.findRoutes(page, size, filter, sortBy, direction);
    }

    public RouteResponse findRoute(long id) {
        return routes.findRoute(id);
    }

    public RouteResponse createRoute(RouteRequest request) {
        RouteResponse route = routes.createRoute(request);
        notifications.routesChanged();
        return route;
    }

    public RouteResponse updateRoute(long id, RouteRequest request) {
        RouteResponse route = routes.updateRoute(id, request);
        notifications.routesChanged();
        return route;
    }

    public void deleteRoute(long id) {
        routes.deleteRoute(id);
        notifications.routesChanged();
    }

    public List<LocationDto> findLocations() {
        return locations.findLocations();
    }

    public LocationDto createLocation(LocationRequest request) {
        LocationDto location = locations.createLocation(request);
        notifications.locationsChanged();
        notifications.routesChanged();
        return location;
    }

    public long countLocationRoutes(long id) {
        return locations.countLocationRoutes(id);
    }

    public void deleteLocation(long id, DeleteLocationRequest request) {
        locations.deleteLocation(id, request);
        notifications.locationsChanged();
        notifications.routesChanged();
    }

    public double getAverageRating() {
        return operations.getAverageRating();
    }

    public long countRoutesBelowRating(long rating) {
        return operations.countRoutesBelowRating(rating);
    }

    public List<Long> getUniqueRatings() {
        return operations.getUniqueRatings();
    }

    public RouteResponse getShortestRoute(long fromLocationId, long toLocationId) {
        return operations.getShortestRoute(fromLocationId, toLocationId);
    }

    public RouteResponse addRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        RouteResponse route = operations.addRouteBetweenLocations(request);
        notifications.routesChanged();
        return route;
    }
}
