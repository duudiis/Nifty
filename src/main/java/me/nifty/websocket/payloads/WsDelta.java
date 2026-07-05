package me.nifty.websocket.payloads;

import me.nifty.core.database.BotIdentity;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.DatabaseManager;
import me.nifty.websocket.DashboardSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Granular, data-carrying change events for the dashboard.
 *
 * <p>Where {@link WsUpdates} sends bare "something changed" nudges that make the
 * dashboard re-read the database, these ship the actual change so a dashboard
 * far from the database never has to fetch per change. The queue travels as
 * structural deltas — {@code q_add}/{@code q_remove}/{@code q_move}/{@code
 * q_clear}, each carrying only what moved — while the player travels as a small
 * current-track snapshot ({@code p_full}). Bulk reshuffles and range edits fall
 * back to a {@code q_resync} nudge (the dashboard refetches the queue).</p>
 *
 * <p>Every payload is read straight from the shared database right after the
 * mutation, so its shape matches exactly what a full fetch would return — the
 * two paths can never drift. The bot sits next to the database, so these reads
 * are cheap; the point is to keep the distant dashboard from reading. Pure
 * add-on: every call is a no-op while the dashboard is disconnected.</p>
 */
public class WsDelta {

    /* ---------------- player ---------------- */

    /** Ships the current player snapshot for a guild. */
    public static void player(PlayerManager playerManager) {
        if (playerManager == null || playerManager.getGuild() == null) { return; }
        player(playerManager.getGuild().getIdLong());
    }

    /** Ships the current player snapshot (current track + scalars), or idle. */
    public static void player(long guildId) {
        if (!DashboardSocket.isConnected()) { return; }
        try {
            send("p_full", guildId, playerPayload(guildId));
        } catch (Exception ignored) {
            // Notifications must never break playback.
        }
    }

    /* ---------------- queue ---------------- */

    /** {@code count} contiguous tracks were inserted starting at {@code position}. */
    public static void qAdd(long guildId, int position, int count) {
        if (!DashboardSocket.isConnected() || count <= 0) { return; }
        try {

            JSONArray tracks = new JSONArray();

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT qt.id, qt.position, qt.queued_by, " +
                         "t.title, t.artist, t.artwork_url, t.url, t.duration_ms, " +
                         "u.display_name AS added_by, u.avatar_url AS added_by_avatar " +
                         "FROM queue_tracks qt " +
                         "JOIN tracks t ON t.id = qt.track_id " +
                         "LEFT JOIN users u ON u.id = qt.queued_by " +
                         "WHERE qt.bot_id = ? AND qt.guild_id = ? AND qt.position >= ? AND qt.position < ? " +
                         "ORDER BY qt.position ASC")) {

                statement.setLong(1, BotIdentity.get());
                statement.setLong(2, guildId);
                statement.setInt(3, position);
                statement.setInt(4, position + count);

                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    tracks.put(trackJson(result, true));
                }
            }

            if (tracks.isEmpty()) { return; }

            JSONObject data = new JSONObject();
            data.put("at", position);
            data.put("cursor", cursor(guildId));
            data.put("tracks", tracks);

            send("q_add", guildId, data);

        } catch (Exception ignored) { }
    }

    /** {@code count} tracks were removed starting at {@code position}. */
    public static void qRemove(long guildId, int position, int count) {
        if (!DashboardSocket.isConnected() || count <= 0) { return; }
        try {
            JSONObject data = new JSONObject();
            data.put("at", position);
            data.put("count", count);
            data.put("cursor", cursor(guildId));
            send("q_remove", guildId, data);
        } catch (Exception ignored) { }
    }

    /** A single track moved from {@code from} to {@code to}. */
    public static void qMove(long guildId, int from, int to) {
        if (!DashboardSocket.isConnected() || from == to) { return; }
        try {
            JSONObject data = new JSONObject();
            data.put("from", from);
            data.put("to", to);
            data.put("cursor", cursor(guildId));
            send("q_move", guildId, data);
        } catch (Exception ignored) { }
    }

    /** The queue was emptied. */
    public static void qClear(long guildId) {
        if (!DashboardSocket.isConnected()) { return; }
        send("q_clear", guildId, new JSONObject());
    }

    /**
     * Too many changes at once (a reshuffle or a range edit): the dashboard
     * should refetch the whole queue rather than replay individual moves.
     */
    public static void qResync(long guildId) {
        if (!DashboardSocket.isConnected()) { return; }
        send("q_resync", guildId, new JSONObject());
    }

    /* ---------------- helpers ---------------- */

    /** The queue cursor (current position) straight from the players row. */
    private static int cursor(long guildId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT queue_position FROM players WHERE bot_id = ? AND guild_id = ?")) {
            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);
            ResultSet result = statement.executeQuery();
            if (result.next()) { return result.getInt("queue_position"); }
        } catch (Exception ignored) { }
        return 0;
    }

    /**
     * Reads the player row + current track and computes wall-clock progress
     * exactly like the dashboard's full fetch, so both paths agree.
     */
    private static JSONObject playerPayload(long guildId) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT p.playing, p.track_loaded, p.queue_position, p.loop_mode, p.shuffle, p.volume, p.speed, " +
                     "p.position_ms, (EXTRACT(EPOCH FROM (now() - p.position_at)) * 1000)::bigint AS elapsed_ms, " +
                     "t.title, t.artist, t.artwork_url, t.url, t.duration_ms, " +
                     "qt.queued_by, u.display_name AS added_by, u.avatar_url AS added_by_avatar " +
                     "FROM players p " +
                     "LEFT JOIN queue_tracks qt ON qt.bot_id = p.bot_id AND qt.guild_id = p.guild_id AND qt.position = p.queue_position " +
                     "LEFT JOIN tracks t ON t.id = qt.track_id " +
                     "LEFT JOIN users u ON u.id = qt.queued_by " +
                     "WHERE p.bot_id = ? AND p.guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);

            ResultSet result = statement.executeQuery();
            if (!result.next()) { return idle(); }

            boolean trackLoaded = result.getBoolean("track_loaded");
            String title = result.getString("title");
            if (!trackLoaded || title == null) { return idle(); }

            double speed = result.getDouble("speed");
            if (speed <= 0) { speed = 1; }

            boolean playing = result.getBoolean("playing");

            long durationMs = result.getLong("duration_ms");
            boolean hasDuration = !result.wasNull();

            double progress = result.getLong("position_ms");
            if (playing) { progress += result.getLong("elapsed_ms") * speed; }
            if (hasDuration && durationMs > 0) { progress = Math.min(progress, durationMs); }
            if (progress < 0) { progress = 0; }

            JSONObject data = new JSONObject();
            data.put("progress", Math.round(progress));
            data.put("playing", playing);
            data.put("shuffle", "enabled".equals(result.getString("shuffle")));
            data.put("loop", result.getString("loop_mode"));
            data.put("volume", result.getInt("volume"));
            data.put("speed", speed);
            data.put("position", result.getInt("queue_position"));
            data.put("track", trackJson(result, false));
            return data;

        } catch (Exception ignored) {
            return idle();
        }
    }

    /** The "nothing is playing" payload — an explicit null track. */
    private static JSONObject idle() {
        JSONObject data = new JSONObject();
        data.put("track", JSONObject.NULL);
        return data;
    }

    /**
     * Raw catalog columns for one row; the dashboard applies its own splitTitle
     * so the delta and the full fetch render identically. Snowflake ids travel
     * as strings so the browser never loses precision past 2^53.
     */
    private static JSONObject trackJson(ResultSet result, boolean queueRow) throws Exception {

        JSONObject track = new JSONObject();

        track.put("title", nullable(result.getString("title")));
        track.put("artist", nullable(result.getString("artist")));
        track.put("artwork_url", nullable(result.getString("artwork_url")));
        track.put("url", nullable(result.getString("url")));

        long durationMs = result.getLong("duration_ms");
        track.put("duration_ms", result.wasNull() ? JSONObject.NULL : durationMs);

        long queuedBy = result.getLong("queued_by");
        track.put("queued_by", result.wasNull() ? JSONObject.NULL : String.valueOf(queuedBy));

        track.put("added_by", nullable(result.getString("added_by")));
        track.put("added_by_avatar", nullable(result.getString("added_by_avatar")));

        if (queueRow) {
            track.put("id", String.valueOf(result.getLong("id")));
            track.put("position", result.getInt("position"));
        }

        return track;
    }

    private static Object nullable(String value) {
        return value == null ? JSONObject.NULL : value;
    }

    private static void send(String operation, long guildId, JSONObject data) {

        if (!DashboardSocket.isConnected()) { return; }

        try {

            JSONObject base = new JSONObject();
            base.put("operation", operation);
            base.put("botId", String.valueOf(BotIdentity.get()));
            base.put("guildId", String.valueOf(guildId));
            base.put("data", data);

            DashboardSocket.send(base.toString());

        } catch (Exception ignored) {
            // Notifications must never break playback.
        }

    }

}
