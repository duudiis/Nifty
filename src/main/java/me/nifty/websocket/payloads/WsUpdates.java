package me.nifty.websocket.payloads;

import me.nifty.core.database.BotIdentity;
import me.nifty.core.music.PlayerManager;
import me.nifty.websocket.DashboardSocket;
import org.json.JSONObject;

/**
 * Lightweight change notifications for the dashboard.
 *
 * <p>The shared PostgreSQL database is the source of truth for player and
 * queue state — the dashboard reads it directly. These nudges only tell it
 * <em>when</em> to re-read; they carry no payload beyond the address. Pure
 * add-on: when the dashboard is disconnected every call is a no-op.</p>
 */
public class WsUpdates {

    /** Notifies the dashboard that a guild's player state changed. */
    public static void player(PlayerManager playerManager) {
        if (playerManager == null || playerManager.getGuild() == null) { return; }
        player(playerManager.getGuild().getIdLong());
    }

    /** Notifies the dashboard that a guild's player state changed. */
    public static void player(long guildId) {
        send("player_updated", guildId);
    }

    /** Notifies the dashboard that a guild's queue changed. */
    public static void queue(long guildId) {
        send("queue_updated", guildId);
    }

    private static void send(String operation, long guildId) {

        if (!DashboardSocket.isConnected()) { return; }

        try {

            JSONObject base = new JSONObject();
            base.put("operation", operation);
            base.put("botId", String.valueOf(BotIdentity.get()));
            base.put("guildId", String.valueOf(guildId));

            DashboardSocket.send(base.toString());

        } catch (Exception ignored) {
            // Notifications must never break playback.
        }

    }

}
