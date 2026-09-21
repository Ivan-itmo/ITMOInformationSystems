package ru.itmo.routes.dto;

import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.model.Route;

public final class Mapper {
    private Mapper() {
    }

    public static CoordinatesDto toDto(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        return new CoordinatesDto(coordinates.getId(), coordinates.getX(), coordinates.getY());
    }

    public static LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDto(location.getId(), location.getX(), location.getY(), location.getZ());
    }

    public static RouteResponse toDto(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                toDto(route.getCoordinates()),
                route.getCreationDate(),
                toDto(route.getFrom()),
                toDto(route.getTo()),
                route.getDistance(),
                route.getRating()
        );
    }
}
