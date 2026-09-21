package ru.itmo.routes.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import ru.itmo.routes.api.RouteUpdatesEndpoint;

@WebListener
public class ApplicationLifecycle implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        RouteUpdatesEndpoint.closeAll();
    }
}
