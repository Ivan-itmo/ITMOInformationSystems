package ru.itmo.routes.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.lang.reflect.Proxy;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.model.*;

import static org.junit.jupiter.api.Assertions.*;

class LocationDeletionTest {
    private static SessionFactory factory;
    private EntityManager em;
    private RouteService service;

    @BeforeAll
    static void database() {
        factory = new Configuration()
                .addAnnotatedClass(Route.class).addAnnotatedClass(Location.class).addAnnotatedClass(Coordinates.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:deletion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
    }

    @AfterAll
    static void closeDatabase() { factory.close(); }

    @BeforeEach
    void setup() throws Exception {
        em = factory.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("alter table route alter column creation_date set default current_timestamp").executeUpdate();
        service = new RouteService();
        inject("entityManager", em);
        inject("changeNotifier", new RouteChangeNotifier());
        inject("transactionSynchronizationRegistry", Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{TransactionSynchronizationRegistry.class},
                (proxy, method, args) -> null));
    }

    private void inject(String name, Object value) throws Exception {
        var field = RouteService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    @AfterEach
    void cleanup() {
        em.getTransaction().rollback();
        em.close();
    }

    private Location location() {
        Location location = new Location();
        em.persist(location);
        return location;
    }

    private Route route(Location from, Location to) {
        Route route = new Route();
        route.setName("Test");
        route.setFrom(from);
        route.setTo(to);
        route.setCoordinates(new Coordinates());
        route.setDistance(10.0);
        route.setRating(1L);
        em.persist(route);
        em.flush();
        return route;
    }

    @Test
    void movesBothDirectionsAndPreservesUnrelatedRoutes() {
        Location source = location(), replacement = location(), other = location();
        long sourceId = source.getId(), replacementId = replacement.getId(), otherId = other.getId();
        long fromId = route(source, other).getId();
        long toId = route(null, source).getId();
        long bothId = route(source, source).getId();
        long unrelatedId = route(other, replacement).getId();
        em.clear();
        assertEquals(3, service.countLocationRoutes(sourceId));
        service.deleteLocation(sourceId, new DeleteLocationRequest(replacementId));
        em.clear();
        assertNull(em.find(Location.class, sourceId));
        assertEquals(replacementId, em.find(Route.class, fromId).getFrom().getId());
        assertEquals(otherId, em.find(Route.class, fromId).getTo().getId());
        assertNull(em.find(Route.class, toId).getFrom());
        assertEquals(replacementId, em.find(Route.class, toId).getTo().getId());
        assertEquals(replacementId, em.find(Route.class, bothId).getFrom().getId());
        assertEquals(replacementId, em.find(Route.class, bothId).getTo().getId());
        assertEquals(otherId, em.find(Route.class, unrelatedId).getFrom().getId());
        assertEquals(replacementId, em.find(Route.class, unrelatedId).getTo().getId());
    }

    @Test
    void rejectsMissingReplacementEvenForOptionalFrom() {
        Location source = location(), other = location();
        long routeId = route(source, other).getId();
        assertThrows(ValidationException.class, () -> service.deleteLocation(source.getId(), null));
        em.clear();
        assertNotNull(em.find(Location.class, source.getId()));
        assertEquals(source.getId(), em.find(Route.class, routeId).getFrom().getId());
    }

    @Test
    void rejectsSameOrUnknownReplacement() {
        Location source = location();
        assertThrows(ValidationException.class, () -> service.deleteLocation(source.getId(), new DeleteLocationRequest(source.getId())));
        assertThrows(NotFoundException.class, () -> service.deleteLocation(source.getId(), new DeleteLocationRequest(Long.MAX_VALUE)));
        assertNotNull(em.find(Location.class, source.getId()));
    }

    @Test
    void deletesUnusedLocationWithoutReplacement() {
        long id = location().getId();
        service.deleteLocation(id, null);
        em.clear();
        assertNull(em.find(Location.class, id));
    }

    @Test
    void rejectsUnknownSource() {
        assertThrows(NotFoundException.class, () -> service.deleteLocation(Long.MAX_VALUE, null));
    }

    @Test
    void deletingRoutePreservesSharedLocationsAndOtherRoutes() {
        Location from = location(), to = location();
        Route removed = route(from, to);
        long removedId = removed.getId(), coordinatesId = removed.getCoordinates().getId();
        long retainedId = route(from, to).getId();
        service.deleteRoute(removedId);
        em.flush();
        em.clear();
        assertNull(em.find(Route.class, removedId));
        assertNull(em.find(Coordinates.class, coordinatesId));
        assertNotNull(em.find(Location.class, from.getId()));
        assertNotNull(em.find(Location.class, to.getId()));
        assertEquals(from.getId(), em.find(Route.class, retainedId).getFrom().getId());
        assertEquals(to.getId(), em.find(Route.class, retainedId).getTo().getId());
    }
}
