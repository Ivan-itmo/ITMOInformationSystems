package ru.itmo.routes.service.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationDto;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.Mapper;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.RouteValidator;

@ApplicationScoped
public class LocationService {
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    @Inject
    private RouteValidator validator;

    @Inject
    private RouteEntityManager entities;

    @Inject
    private RouteTransactionNotifier notifications;

    public List<LocationDto> findLocations() {
        return entityManager.createQuery("select l from Location l order by l.id", Location.class)
                .getResultStream()
                .map(Mapper::toDto)
                .toList();
    }

    @Transactional
    public LocationDto createLocation(LocationRequest request) {
        validator.validateLocation(request);
        Location location = new Location();
        location.setX(request.x());
        location.setY(request.y());
        location.setZ(request.z());
        entityManager.persist(location);
        notifications.locationsChangedAfterCommit();
        return Mapper.toDto(location);
    }

    public long countLocationRoutes(long id) {
        return countLocationRoutes(entities.findLocationEntity(id));
    }

    private long countLocationRoutes(Location location) {
        return entityManager.createQuery(
                "select count(r) from Route r where r.from = :location or r.to = :location", Long.class)
                .setParameter("location", location)
                .getSingleResult();
    }

    @Transactional
    public void deleteLocation(long id, DeleteLocationRequest request) {
        Location location = entities.findLocationEntity(id); 
        Long replacementId = request == null ? null : request.replacementLocationId();
        validator.validateReplacementLocation(id, replacementId);
        Location replacement = replacementId == null ? null : entities.findLocationEntity(replacementId);
        validator.validateLocationDeletion(countLocationRoutes(location), replacement != null);
    
        if (replacement != null) {
            entityManager.createQuery("update Route r set r.from = :replacement where r.from = :location")
                    .setParameter("replacement", replacement)
                    .setParameter("location", location)
                    .executeUpdate();
            entityManager.createQuery("update Route r set r.to = :replacement where r.from = :location")
                    .setParameter("replacement", replacement)
                    .setParameter("location", location)
                    .executeUpdate();
        }
        entityManager.remove(location);
        entityManager.flush();
        notifications.locationsChangedAfterCommit();
    }
}
