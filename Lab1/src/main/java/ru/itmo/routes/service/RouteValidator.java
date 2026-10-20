package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.CoordinatesRequest;
import ru.itmo.routes.dto.RouteRequest;

@ApplicationScoped
public class RouteValidator {
    public void validateRoute(RouteRequest request) {
        if (request == null) {
            throw new ValidationException("Данные маршрута обязательны");
        }
        validateFields(request.name(), request.coordinates(), request.fromLocationId(), request.toLocationId(), request.distance(), request.rating());
    }

    public void validateRouteBetweenLocations(AddRouteBetweenLocationsRequest request) {
        if (request == null) {
            throw new ValidationException("Данные маршрута обязательны");
        }
        validateFields(request.name(), request.coordinates(), request.fromLocationId(), request.toLocationId(), request.distance(), request.rating());
    }

    private void validateFields(String name, CoordinatesRequest coordinates, Long fromId, Long toId, Double distance, Long rating) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Название маршрута не может быть пустым");
        }
        if (name.length() > 255) {
            throw new ValidationException("Название маршрута не должно превышать 255 символов");
        }
        if (coordinates == null || coordinates.x() == null || coordinates.y() == null
                || !Double.isFinite(coordinates.x()) || !Double.isFinite(coordinates.y())) {
            throw new ValidationException("Координаты маршрута обязательны и должны быть конечными числами");
        }
        if (toId == null || toId <= 0 || (fromId != null && fromId <= 0)) {
            throw new ValidationException("ID локации должен быть целым числом больше 0; локация назначения обязательна");
        }
        if (distance == null || !Double.isFinite(distance) || distance <= 0) {
            throw new ValidationException("Дистанция должна быть конечным числом больше 0");
        }
        if (rating == null || rating <= 0) {
            throw new ValidationException("Рейтинг должен быть целым числом больше 0");
        }
    }
}
