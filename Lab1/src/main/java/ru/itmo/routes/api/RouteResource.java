package ru.itmo.routes.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationDto;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.PageResponse;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.dto.RouteResponse;
import ru.itmo.routes.service.route.RouteManager;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteResource {
    @Inject
    private RouteManager routeManager;

    @GET
    @Path("routes")
    public PageResponse<RouteResponse> findRoutes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("filter") String filter,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("direction") @DefaultValue("asc") String direction
    ) {
        return routeManager.findRoutes(page, size, filter, sortBy, direction);
    }

    @GET
    @Path("routes/{id}")
    public RouteResponse findRoute(@PathParam("id") long id) {
        return routeManager.findRoute(id);
    }

    @POST
    @Path("routes")
    public RouteResponse createRoute(RouteRequest request) {
        return routeManager.createRoute(request);
    }

    @PUT
    @Path("routes/{id}")
    public RouteResponse updateRoute(@PathParam("id") long id, RouteRequest request) {
        return routeManager.updateRoute(id, request);
    }

    @DELETE
    @Path("routes/{id}")
    public void deleteRoute(@PathParam("id") long id) {
        routeManager.deleteRoute(id);
    }

    @GET
    @Path("locations")
    public List<LocationDto> findLocations() {
        return routeManager.findLocations();
    }

    @GET
    @Path("locations/{id}/usage")
    public Map<String, Long> locationUsage(@PathParam("id") long id) {
        return Map.of("routeCount", routeManager.countLocationRoutes(id));
    }

    @DELETE
    @Path("locations/{id}")
    public void deleteLocation(@PathParam("id") long id, DeleteLocationRequest request) {
        routeManager.deleteLocation(id, request);
    }

    @POST
    @Path("locations")
    public LocationDto createLocation(LocationRequest request) {
        return routeManager.createLocation(request);
    }

    @GET
    @Path("operations/avg-rating")
    public Map<String, Double> getAverageRating() {
        return Map.of("value", routeManager.getAverageRating());
    }

    @GET
    @Path("operations/count-below-rating")
    public Map<String, Long> countRoutesBelowRating(@QueryParam("rating") long rating) {
        return Map.of("value", routeManager.countRoutesBelowRating(rating));
    }

    @GET
    @Path("operations/unique-ratings")
    public Map<String, List<Long>> getUniqueRatings() {
        return Map.of("value", routeManager.getUniqueRatings());
    }

    @GET
    @Path("operations/shortest-route")
    public RouteResponse getShortestRoute(@QueryParam("fromLocationId") long fromLocationId, @QueryParam("toLocationId") long toLocationId) {
        return routeManager.getShortestRoute(fromLocationId, toLocationId);
    }

    @POST
    @Path("operations/add-between-locations")
    public RouteResponse addRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        return routeManager.addRouteBetweenLocations(request);
    }
}
