package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.routes.dto.LocationRequest;

@ApplicationScoped
public class LocationValidator {
    public void validateLocation(LocationRequest request) {
        if (request == null || !isFinite(request.x()) || !isFinite(request.y()) || !isFinite(request.z())) {
            throw new ValidationException("Координаты локации обязательны и должны быть конечными числами");
        }
    }

    public void validateReplacementLocation(long locationId, Long replacementId) {
        if (replacementId != null && replacementId <= 0) {
            throw new ValidationException("ID локации для замены должен быть больше 0");
        }
        if (replacementId != null && replacementId == locationId) {
            throw new ValidationException("Выберите другую локацию для замены");
        }
    }

    public void validateLocationDeletion(long routeCount, boolean hasReplacement) {
        if (routeCount > 0 && !hasReplacement) {
            throw new ValidationException("Локация используется маршрутами. Выберите локацию для замены");
        }
    }

    private boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }
}
