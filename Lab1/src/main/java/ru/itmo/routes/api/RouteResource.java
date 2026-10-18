package ru.itmo.routes.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
import ru.itmo.routes.service.NumericInputLimits;
import ru.itmo.routes.validation.PositiveLong;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteResource {
    @Inject
    private RouteManager routeManager;

    @GET
    @Path("routes")
    public PageResponse<RouteResponse> findRoutes(
            @QueryParam("page") @DefaultValue("0") @NotBlank @Size(max = NumericInputLimits.max_length)
            @Pattern(regexp = "[0-9]+") @DecimalMin("0") @DecimalMax("2147483647") String page,
            @QueryParam("size") @DefaultValue("10") @NotBlank @Size(max = NumericInputLimits.max_length)
            @Pattern(regexp = "[0-9]+") @DecimalMin("1") @DecimalMax("2147483647") String size,
            @QueryParam("filter") String filter,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("direction") @DefaultValue("asc") String direction
    ) {
        return routeManager.findRoutes(Integer.parseInt(page), Integer.parseInt(size), filter, sortBy, direction);
    }

    @GET
    @Path("routes/{id}")
    public RouteResponse findRoute(@PathParam("id") @PositiveLong String id) {
        return routeManager.findRoute(Long.parseLong(id));
    }

    @POST
    @Path("routes")
    public RouteResponse createRoute(@NotNull @Valid RouteRequest request) {
        return routeManager.createRoute(request);
    }

    @PUT
    @Path("routes/{id}")
    public RouteResponse updateRoute(@PathParam("id") @PositiveLong String id, @NotNull @Valid RouteRequest request) {
        return routeManager.updateRoute(Long.parseLong(id), request);
    }

    @DELETE
    @Path("routes/{id}")
    public void deleteRoute(@PathParam("id") @PositiveLong String id) {
        routeManager.deleteRoute(Long.parseLong(id));
    }

    @GET
    @Path("locations")
    public List<LocationDto> findLocations() {
        return routeManager.findLocations();
    }

    @GET
    @Path("locations/{id}/usage")
    public Map<String, Long> locationUsage(@PathParam("id") @PositiveLong String id) {
        return Map.of("routeCount", routeManager.countLocationRoutes(Long.parseLong(id)));
    }

    @DELETE
    @Path("locations/{id}")
    public void deleteLocation(@PathParam("id") @PositiveLong String id, @Valid DeleteLocationRequest request) {
        routeManager.deleteLocation(Long.parseLong(id), request);
    }

    @POST
    @Path("locations")
    public LocationDto createLocation(@NotNull @Valid LocationRequest request) {
        return routeManager.createLocation(request);
    }

    @GET
    @Path("operations/avg-rating")
    public Map<String, Double> getAverageRating() {
        return Map.of("value", routeManager.getAverageRating());
    }

    @GET
    @Path("operations/count-below-rating")
    public Map<String, Long> countRoutesBelowRating(@QueryParam("rating") @PositiveLong String rating) {
        return Map.of("value", routeManager.countRoutesBelowRating(Long.parseLong(rating)));
    }

    @GET
    @Path("operations/unique-ratings")
    public Map<String, List<Long>> getUniqueRatings() {
        return Map.of("value", routeManager.getUniqueRatings());
    }

    @GET
    @Path("operations/shortest-route")
    public RouteResponse getShortestRoute(@QueryParam("fromLocationId") @PositiveLong String fromLocationId, @QueryParam("toLocationId") @PositiveLong String toLocationId) {
        return routeManager.getShortestRoute(Long.parseLong(fromLocationId), Long.parseLong(toLocationId));
    }

    @POST
    @Path("operations/add-between-locations")
    public RouteResponse addRouteBetweenLocations(@NotNull @Valid AddRouteBetweenLocationsRequest request) {
        return routeManager.addRouteBetweenLocations(request);
    }

}
