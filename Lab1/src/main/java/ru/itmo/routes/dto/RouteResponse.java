package ru.itmo.routes.dto;

import java.time.LocalDateTime;

public record RouteResponse(
        Long id,
        String name,
        CoordinatesDto coordinates,
        LocalDateTime creationDate,
        LocationDto from,
        LocationDto to,
        Double distance,
        Long rating
) {
}
