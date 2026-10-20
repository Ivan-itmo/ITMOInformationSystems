package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import ru.itmo.routes.api.NumberInputDeserializer;

public record AddRouteBetweenLocationsRequest(
        String name,
        CoordinatesRequest coordinates,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Long fromLocationId,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Long toLocationId,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Double distance,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Long rating
) {
}
