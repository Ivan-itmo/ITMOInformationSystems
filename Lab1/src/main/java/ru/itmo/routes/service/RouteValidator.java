package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RouteValidator {
    public void validateReplacementLocation(long locationId, Long replacementId) {
        if (replacementId != null && replacementId == locationId) {
            throw new ValidationException("Выберите другую локацию для замены");
        }
    }

    public void validateLocationDeletion(long routeCount, boolean hasReplacement) {
        if (routeCount > 0 && !hasReplacement) {
            throw new ValidationException("Локация используется маршрутами. Выберите локацию для замены");
        }
    }
}
