package me.nifty.websocket;

import me.nifty.core.database.BotIdentity;
import me.nifty.websocket.payloads.WsSessions;
import org.json.JSONObject;

/**
 * Routes inbound dashboard messages: protocol operations are handled here,
 * player control is delegated to {@link DashboardActions}.
 *
 * <p>With several bot instances connected to one dashboard, messages may carry
 * a {@code botId}. Messages addressed to another instance are dropped; messages
 * without a botId are treated as addressed to every instance (which keeps older
 * dashboards working).</p>
 */
public class DashboardRouter {

    static void route(JSONObject json) {

        String operation = json.optString("operation", "");
        JSONObject data = json.optJSONObject("data");
        if (data == null) { data = new JSONObject(); }

        // Addressed to a different bot instance — not ours to handle.
        String targetBotId = data.optString("botId", "");
        if (!targetBotId.isEmpty() && !targetBotId.equals(String.valueOf(BotIdentity.get()))) { return; }

        switch (operation) {

            case "hello" -> DashboardSocket.startHeartbeat(data.optInt("heartbeatInterval", 45_000));

            case "heartbeat_ack" -> { /* keep-alive acknowledged */ }

            case "identify_success" -> System.out.println("[Nifty] Dashboard authenticated this bot.");

            case "identify_error" -> System.out.println("[Nifty] Dashboard rejected this bot's token.");

            // The dashboard asks every connected bot which sessions a user can control.
            // Player and queue state need no equivalent: the dashboard reads
            // those directly from the shared database.
            case "sessions_request" -> WsSessions.reply(data.optLong("userId", 0));

            case "action" -> DashboardActions.handle(data);

            default -> { /* unknown operation — ignore */ }

        }

    }

}
