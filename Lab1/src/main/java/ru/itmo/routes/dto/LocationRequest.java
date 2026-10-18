package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.validation.constraints.NotNull;
import ru.itmo.routes.api.NumberInputDeserializer;
import ru.itmo.routes.validation.Finite;

public record LocationRequest(
        @NotNull @Finite @JsonbTypeDeserializer(NumberInputDeserializer.class) Double x,
        @NotNull @Finite @JsonbTypeDeserializer(NumberInputDeserializer.class) Double y,
        @NotNull @Finite @JsonbTypeDeserializer(NumberInputDeserializer.class) Double z
) {
}
