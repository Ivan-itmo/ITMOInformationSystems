package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.itmo.routes.model.Coordinates;

@ApplicationScoped
public class CoordinatesRepository {
    @PersistenceContext(unitName = "routesPU")
    private EntityManager entityManager;

    public void persist(Coordinates coordinates) {
        entityManager.persist(coordinates);
        entityManager.flush();
    }
}
