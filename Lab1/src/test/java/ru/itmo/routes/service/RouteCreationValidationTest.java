package ru.itmo.routes.service;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.CoordinatesDto;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.model.Location;

import static org.junit.jupiter.api.Assertions.*;

class RouteCreationValidationTest {
    private final RouteService service = new RouteService();

    private AddRouteBetweenLocationsRequest request(Long from, Long to) {
        return new AddRouteBetweenLocationsRequest("Test", new CoordinatesDto(null, 1.0, 2.0), from, to, 10.0, 1L);
    }

    @Test
    void rejectsInvalidIdsBeforeAccessingDatabase() {
        for (Long[] ids : new Long[][]{{null, null}, {null, 0L}, {null, -1L}, {0L, 1L}, {-1L, 1L}}) {
            var request = request(ids[0], ids[1]);
            assertThrows(ValidationException.class, () -> service.addRouteBetweenLocations(request));
            assertThrows(ValidationException.class, () -> service.createRoute(new RouteRequest(
                    request.name(), request.coordinates(), ids[0], ids[1], request.distance(), request.rating())));
        }
        assertThrows(ValidationException.class, () -> service.addRouteBetweenLocations(null));
    }

    @Test
    void rejectsUnknownLocationsBeforeAnyWritesOrSqlFunctionCall() throws Exception {
        var em = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("find")) {
                        assertEquals(Location.class, args[0]);
                        return args[1].equals(1L) ? new Location() : null;
                    }
                    throw new AssertionError("Unexpected database operation: " + method.getName());
                });
        var field = RouteService.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(service, em);

        assertThrows(NotFoundException.class, () -> service.addRouteBetweenLocations(request(2L, 1L)));
        assertThrows(NotFoundException.class, () -> service.addRouteBetweenLocations(request(1L, 2L)));
        assertThrows(NotFoundException.class, () -> service.addRouteBetweenLocations(request(null, 2L)));
    }
}
