package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import ru.itmo.routes.api.NumberInputDeserializer;

public record LocationRequest(
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Double x,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Double y,
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Double z
) {
}
