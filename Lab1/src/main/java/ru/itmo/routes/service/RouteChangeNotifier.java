package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.routes.api.RouteUpdatesEndpoint;

@ApplicationScoped
public class RouteChangeNotifier {
    public void routesChanged() {
        RouteUpdatesEndpoint.broadcast("routes-changed");
    }

    public void locationsChanged() {
        RouteUpdatesEndpoint.broadcast("locations-changed");
    }
}
