package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.RouteRequest;

@ApplicationScoped
public class RouteValidator {
    public void validateRoute(RouteRequest request) {
        if (request == null) {
            throw new ValidationException("Данные маршрута обязательны");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ValidationException("Название маршрута не может быть пустым");
        }
        if (request.coordinates() == null || !isFinite(request.coordinates().x()) || !isFinite(request.coordinates().y())) {
            throw new ValidationException("Координаты маршрута обязательны");
        }
        if (request.toLocationId() == null) {
            throw new ValidationException("Локация назначения обязательна");
        }
        if (request.toLocationId() <= 0 || (request.fromLocationId() != null && request.fromLocationId() <= 0)) {
            throw new ValidationException("ID локации должен быть целым числом больше 0");
        }
        if (request.distance() == null || !Double.isFinite(request.distance()) || request.distance() <= 0) {
            throw new ValidationException("Дистанция должна быть больше 0");
        }
        if (request.rating() == null || request.rating() <= 0) {
            throw new ValidationException("Рейтинг должен быть больше 0");
        }
    }

    public void validateLocation(LocationRequest request) {
        if (request == null || !isFinite(request.x()) || !isFinite(request.y()) || !isFinite(request.z())) {
            throw new ValidationException("Координаты локации должны быть конечными числами");
        }
    }

    private boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    public void validateRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        if (request == null) {
            throw new ValidationException("Данные маршрута обязательны");
        }
        validateRoute(new RouteRequest(
                request.name(),
                request.coordinates(),
                request.fromLocationId(),
                request.toLocationId(),
                request.distance(),
                request.rating()
        ));
    }

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
