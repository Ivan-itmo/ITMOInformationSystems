package ru.itmo.routes.api;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/updates")
public class RouteUpdatesEndpoint {
    private static final Set<Session> activeSessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        activeSessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        activeSessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (session != null) {
            activeSessions.remove(session);
        }
    }

    public static void broadcast(String message) {
        activeSessions.removeIf(session -> !session.isOpen());
        for (Session session : activeSessions) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException exception) {
                activeSessions.remove(session);
            }
        }
    }

    public static void closeAll() {
        for (Session session : activeSessions) {
            try {
                session.close();
            } catch (IOException exception) {
                activeSessions.remove(session);
            }
        }
        activeSessions.clear();
    }
}
