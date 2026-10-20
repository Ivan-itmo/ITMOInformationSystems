package ru.itmo.routes.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import ru.itmo.routes.api.NumberInputDeserializer;

public record DeleteLocationRequest(
        @JsonbTypeDeserializer(NumberInputDeserializer.class) Long replacementLocationId
) {
}
