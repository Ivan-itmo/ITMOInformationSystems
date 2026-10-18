package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.validation.constraints.Positive;
import ru.itmo.routes.api.NumberInputDeserializer;

public record DeleteLocationRequest(
        @Positive @JsonbTypeDeserializer(NumberInputDeserializer.class) Long replacementLocationId
) {
}
