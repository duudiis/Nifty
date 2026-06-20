package me.nifty.managers;

import me.nifty.Config;
import me.nifty.websocket.WebSocketClientEndpoint;

import java.net.URI;

/**
 * Bootstraps the optional dashboard WebSocket client.
 *
 * <p>The dashboard is a pure add-on. If {@code DASHBOARD_WS_URL} is not set the
 * bot never opens a socket and behaves exactly as a standalone bot. If it is set
 * but the dashboard is offline, the client retries in the background without
 * affecting the bot.</p>
 */
public class WebSocketManager {

    public static void connect() {

        String url = Config.getDashboardWsUrl();

        if (url == null || url.isBlank()) {
            System.out.println("[Nifty] Dashboard disabled (no DASHBOARD_WS_URL set). Running standalone.");
            return;
        }

        try {
            new WebSocketClientEndpoint(new URI(url));
        } catch (Exception e) {
            // Never let dashboard wiring stop the bot from starting.
            System.out.println("[Nifty] Dashboard WebSocket failed to initialise: " + e.getMessage());
        }

    }

}
