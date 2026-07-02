package me.nifty.websocket;

import me.nifty.Config;
import me.nifty.core.database.BotIdentity;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Transport layer of the optional dashboard add-on: owns the WebSocket
 * connection, its heartbeat and reconnection. Inbound messages are handed to
 * {@link DashboardRouter}; what they mean is not this class's business.
 *
 * <p>Intentionally fire-and-forget — every send is guarded, and a dead, slow
 * or broken dashboard can never throw into the bot's audio paths. When the
 * dashboard is unreachable the client keeps retrying in the background while
 * the bot operates normally.</p>
 */
public class DashboardSocket {

    private static volatile WebSocketClient wsClient;
    private static volatile Timer heartbeatTimer;
    private static volatile boolean shuttingDown = false;

    private static final int RECONNECT_DELAY_MS = 10_000;

    /**
     * Opens (and keeps reopening) the connection to the dashboard hub.
     *
     * @param serverURI The dashboard WebSocket URI
     */
    public static void connect(URI serverURI) {

        // Idempotent: ready can fire again after a full gateway reconnect.
        if (wsClient != null) { return; }

        System.out.println("[Nifty] Dashboard WebSocket initialising -> " + serverURI);

        wsClient = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("[Nifty] Dashboard WebSocket connected.");
                identify();
            }

            @Override
            public void onMessage(String message) {
                try {
                    DashboardRouter.route(new JSONObject(message));
                } catch (Exception e) {
                    // A malformed/unexpected dashboard message must never crash the bot.
                    System.out.println("[Nifty] Ignored a malformed dashboard message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                stopHeartbeat();
                if (shuttingDown) { return; }
                System.out.println("[Nifty] Dashboard WebSocket closed (" + code + "). Reconnecting in " + (RECONNECT_DELAY_MS / 1000) + "s...");
                scheduleReconnect();
            }

            @Override
            public void onError(Exception ex) {
                // Swallow: the dashboard being down is not a bot error.
            }

        };

        // Never block the boot thread; connection failures are handled by onClose.
        try {
            wsClient.connect();
        } catch (Exception e) {
            scheduleReconnect();
        }

    }

    /**
     * Sends a message to the dashboard if (and only if) the socket is open.
     * Always safe to call — does nothing when the dashboard is unavailable.
     *
     * @param message The raw message to send.
     */
    public static void send(String message) {
        try {
            WebSocketClient client = wsClient;
            if (client != null && client.isOpen()) {
                client.send(message);
            }
        } catch (Exception ignored) {
            // Never propagate dashboard transport errors.
        }
    }

    /**
     * @return {@code true} if the dashboard socket is currently connected.
     */
    public static boolean isConnected() {
        WebSocketClient client = wsClient;
        return client != null && client.isOpen();
    }

    /**
     * Authenticates this bot instance with the dashboard hub. The id is the
     * bot's Discord user id — the same id that scopes its rows in the shared
     * database, so the dashboard can join both worlds.
     */
    private static void identify() {
        JSONObject identify = new JSONObject();
        identify.put("operation", "identify");

        JSONObject data = new JSONObject();
        data.put("token", Config.getDashboardToken());
        data.put("botId", String.valueOf(BotIdentity.get()));
        data.put("botName", Config.getDashboardBotName());

        identify.put("data", data);

        send(identify.toString());
    }

    /* -------------------------------------------------------------------- */
    /*  Heartbeat & reconnect                                               */
    /* -------------------------------------------------------------------- */

    /**
     * (Re)starts the keep-alive loop at the interval the hub requested.
     *
     * @param interval The heartbeat interval in milliseconds
     */
    static void startHeartbeat(int interval) {

        stopHeartbeat();

        Timer timer = new Timer("dashboard-heartbeat", true);
        heartbeatTimer = timer;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("operation", "heartbeat");
                send(heartbeat.toString());
            }
        }, 0, Math.max(1_000, interval));

    }

    private static void stopHeartbeat() {
        Timer timer = heartbeatTimer;
        if (timer != null) {
            timer.cancel();
            timer.purge();
            heartbeatTimer = null;
        }
    }

    private static void scheduleReconnect() {
        new Timer("dashboard-reconnect", true).schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    WebSocketClient client = wsClient;
                    if (client != null && !shuttingDown) {
                        client.reconnect();
                    }
                } catch (Exception ignored) { }
            }
        }, RECONNECT_DELAY_MS);
    }

    /**
     * Cleanly stops the dashboard client (used on shutdown). Optional.
     */
    public static void shutdown() {
        shuttingDown = true;
        stopHeartbeat();
        try {
            WebSocketClient client = wsClient;
            if (client != null) { client.close(); }
        } catch (Exception ignored) { }
    }

}
