package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.itmo.routes.api.NumberInputDeserializer;
import ru.itmo.routes.validation.Finite;

public record RouteRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Valid CoordinatesRequest coordinates,
        @Positive @JsonbTypeDeserializer(NumberInputDeserializer.class) Long fromLocationId,
        @NotNull @Positive @JsonbTypeDeserializer(NumberInputDeserializer.class) Long toLocationId,
        @NotNull @Positive @Finite @JsonbTypeDeserializer(NumberInputDeserializer.class) Double distance,
        @NotNull @Positive @JsonbTypeDeserializer(NumberInputDeserializer.class) Long rating,
        RouteResponse expectedState
) {
}
