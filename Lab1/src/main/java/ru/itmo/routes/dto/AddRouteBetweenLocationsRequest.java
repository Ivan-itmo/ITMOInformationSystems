package ru.itmo.routes.dto;

public record AddRouteBetweenLocationsRequest(
        String name,
        CoordinatesDto coordinates,
        Long fromLocationId,
        Long toLocationId,
        Double distance,
        Long rating
) {
}
