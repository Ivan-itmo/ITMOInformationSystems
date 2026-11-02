package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationDto;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.repository.LocationRepository;
import ru.itmo.routes.repository.RouteRepository;
import ru.itmo.routes.validation.LocationValidator;

@ApplicationScoped
@Transactional
public class LocationService {
    @Inject
    private LocationRepository locations;
    @Inject
    private RouteRepository routes;
    @Inject
    private LocationValidator validator;
    @Inject
    private RouteEntityResolver entities;

    public List<LocationDto> findLocations() {
        return locations.findAll()
                .stream()
                .map(Mapper::toDto)
                .toList();
    }

    public LocationDto createLocation(LocationRequest request) {
        validator.validateLocation(request);
        Location location = new Location();
        location.setX(request.x());
        location.setY(request.y());
        location.setZ(request.z());
        locations.persist(location);
        return Mapper.toDto(location);
    }

    public long countLocationRoutes(long id) {
        return routes.countByLocation(entities.findLocationEntity(id));
    }

    public void deleteLocation(long id, DeleteLocationRequest request) {
        Location location = entities.findLocationEntity(id);
        Long replacementId = request == null ? null : request.replacementLocationId();
        validator.validateReplacementLocation(id, replacementId);
        Location replacement = replacementId == null ? null : entities.findLocationEntity(replacementId);
        validator.validateLocationDeletion(routes.countByLocation(location), replacement != null);

        if (replacement != null) {
            routes.replaceLocation(location, replacement);
        }
        locations.delete(location);
    }
}
