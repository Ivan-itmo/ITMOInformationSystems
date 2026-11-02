package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import ru.itmo.routes.model.Location;

@ApplicationScoped
public class LocationRepository {
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    public List<Location> findAll() {
        return entityManager.createQuery("select l from Location l order by l.id", Location.class)
                .getResultList();
    }

    public Location findById(long id) {
        return entityManager.find(Location.class, id);
    }

    public void persist(Location location) {
        entityManager.persist(location);
    }

    public void delete(Location location) {
        entityManager.remove(location);
        entityManager.flush();
    }
}
