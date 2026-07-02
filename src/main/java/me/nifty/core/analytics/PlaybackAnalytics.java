package me.nifty.core.analytics;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.GuildStore;
import me.nifty.core.database.TrackStore;
import me.nifty.core.database.UserStore;
import me.nifty.core.database.UserStore.UserRef;
import me.nifty.managers.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Analytics for one guild's playback: the queue session, its plays, and each
 * user's listening segments.
 *
 * <p>Data model per session (see migrations/001_init.sql):</p>
 * <ul>
 *   <li>{@code queue_sessions} — one row per queue lifetime (create → destroy)</li>
 *   <li>{@code queue_history} — one row per enqueued track (command/dashboard/autoplay)</li>
 *   <li>{@code track_plays} — one row per actual playback of a track</li>
 *   <li>{@code listening_segments} — one row per continuous span a user heard a
 *       play, with start/stop reasons (pause, deafen, move, ...)</li>
 * </ul>
 *
 * <p>Every method snapshots what it needs from JDA on the caller thread and
 * submits the database work to a single shared writer thread. The serial
 * executor guarantees ordering (a session insert always lands before the plays
 * that reference it) and keeps audio/gateway threads free of database I/O.
 * Session and play ids are therefore only touched on the writer thread.</p>
 */
public class PlaybackAnalytics {

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nifty-analytics");
        thread.setDaemon(true);
        return thread;
    });

    private final long guildId;

    // Only read/written on the writer thread (except construction).
    private long sessionId = -1;
    private long playId = -1;

    // Set on event threads, consumed when the matching track-end arrives.
    private volatile boolean skipPending = false;

    public PlaybackAnalytics(long guildId) {
        this.guildId = guildId;
    }

    /* -------------------------------------------------------------------- */
    /*  Session lifecycle                                                   */
    /* -------------------------------------------------------------------- */

    /**
     * Opens a queue session (bot joined voice / player created) and points the
     * live player row at it.
     */
    public void startSession() {
        WRITER.execute(() -> {

            GuildStore.ensure(this.guildId);

            try (Connection connection = DatabaseManager.getConnection()) {

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO queue_sessions (bot_id, guild_id) VALUES (?, ?) RETURNING id")) {
                    statement.setLong(1, BotIdentity.get());
                    statement.setLong(2, this.guildId);

                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        this.sessionId = result.getLong(1);
                    }
                }

                if (this.sessionId != -1) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE players SET session_id = ? WHERE bot_id = ? AND guild_id = ?")) {
                        statement.setLong(1, this.sessionId);
                        statement.setLong(2, BotIdentity.get());
                        statement.setLong(3, this.guildId);
                        statement.executeUpdate();
                    }
                }

            } catch (Exception ignored) { }

        });
    }

    /**
     * Records which voice channel the session lives in (known once the bot's
     * voice update arrives, and again when it gets moved).
     *
     * @param voiceChannelId The session's current voice channel
     */
    public void updateVoiceChannel(long voiceChannelId) {
        WRITER.execute(() -> {

            if (this.sessionId == -1) { return; }

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE queue_sessions SET voice_channel_id = ? WHERE id = ?")) {

                statement.setLong(1, voiceChannelId);
                statement.setLong(2, this.sessionId);
                statement.executeUpdate();

            } catch (Exception ignored) { }

        });
    }

    /**
     * Closes the session (bot left voice / player destroyed). Any segments or
     * play still open are closed defensively — normally the preceding
     * track-end already did that.
     */
    public void endSession() {
        WRITER.execute(() -> {

            if (this.playId != -1) {
                closeOpenSegments("bot_leave");
                closePlay("stopped", null);
            }

            if (this.sessionId == -1) { return; }

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE queue_sessions SET ended_at = now() WHERE id = ? AND ended_at IS NULL")) {

                statement.setLong(1, this.sessionId);
                statement.executeUpdate();

            } catch (Exception ignored) { }

            this.sessionId = -1;

        });
    }

    /* -------------------------------------------------------------------- */
    /*  Queue history                                                       */
    /* -------------------------------------------------------------------- */

    /**
     * Records an enqueued track with the user's fresh profile.
     *
     * @param user The user who queued it
     * @param track The queued track
     * @param via 'command', 'dashboard' or 'autoplay'
     */
    public void trackQueued(UserRef user, AudioTrack track, String via) {
        WRITER.execute(() -> {
            UserStore.upsert(user);
            insertHistory(user.id(), track, via);
        });
    }

    /**
     * Records an enqueued track for a user already known to exist (used by
     * autoplay, which queues as the bot itself).
     *
     * @param userId The queuing user's id
     * @param track The queued track
     * @param via 'command', 'dashboard' or 'autoplay'
     */
    public void trackQueued(long userId, AudioTrack track, String via) {
        WRITER.execute(() -> {
            UserStore.ensureExists(userId);
            insertHistory(userId, track, via);
        });
    }

    private void insertHistory(long userId, AudioTrack track, String via) {

        if (this.sessionId == -1) { return; }

        long trackId = TrackStore.upsert(track);
        if (trackId == -1) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO queue_history (session_id, guild_id, user_id, track_id, via) VALUES (?, ?, ?, ?, ?)")) {

            statement.setLong(1, this.sessionId);
            statement.setLong(2, this.guildId);
            statement.setLong(3, userId);
            statement.setLong(4, trackId);
            statement.setString(5, via);
            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    /* -------------------------------------------------------------------- */
    /*  Plays                                                               */
    /* -------------------------------------------------------------------- */

    /**
     * Opens a play row for a track that just started, and — when the player is
     * audible — a listening segment for everyone currently in the channel.
     *
     * @param track The track that started
     * @param listeners Snapshot of eligible listeners in the bot's channel
     * @param audible Whether users can actually hear it (not paused/muted)
     */
    public void playStarted(AudioTrack track, List<UserRef> listeners, boolean audible) {

        long queuedBy = track.getUserData() instanceof Long ? (Long) track.getUserData() : 0L;

        WRITER.execute(() -> {

            if (this.sessionId == -1) { return; }

            // A lingering play means we missed an end event — close it first.
            if (this.playId != -1) {
                closeOpenSegments("replace");
                closePlay("replaced", null);
            }

            long trackId = TrackStore.upsert(track);
            if (trackId == -1) { return; }

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO track_plays (session_id, guild_id, track_id, queued_by) VALUES (?, ?, ?, ?) RETURNING id")) {

                statement.setLong(1, this.sessionId);
                statement.setLong(2, this.guildId);
                statement.setLong(3, trackId);

                if (queuedBy == 0L || queuedBy == BotIdentity.get()) {
                    statement.setNull(4, Types.BIGINT);   // autoplay
                } else {
                    UserStore.ensureExists(queuedBy);
                    statement.setLong(4, queuedBy);
                }

                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    this.playId = result.getLong(1);
                }

            } catch (Exception ignored) { }

            if (audible) {
                openSegments(listeners, "track_start");
            }

        });
    }

    /**
     * Closes the current play and all of its open listening segments.
     *
     * @param endReason The lavaplayer end reason
     * @param playedMs How far into the track playback got
     */
    public void playEnded(AudioTrackEndReason endReason, long playedMs) {

        boolean skipped = this.skipPending;
        this.skipPending = false;

        String playReason = switch (endReason) {
            case FINISHED -> "finished";
            case LOAD_FAILED -> "error";
            case REPLACED -> skipped ? "skipped" : "replaced";
            default -> skipped ? "skipped" : "stopped";
        };

        String segmentReason = switch (playReason) {
            case "finished" -> "track_finish";
            case "error" -> "error";
            case "skipped", "replaced" -> "replace";
            default -> "stop";
        };

        WRITER.execute(() -> {
            closeOpenSegments(segmentReason);
            closePlay(playReason, playedMs);
        });
    }

    /**
     * Flags that the upcoming track end was a user-initiated skip
     * (skip/back/jump), so it is recorded as 'skipped' rather than 'stopped'.
     */
    public void markSkipped() {
        this.skipPending = true;
    }

    /* -------------------------------------------------------------------- */
    /*  Listening segments                                                  */
    /* -------------------------------------------------------------------- */

    /** Player paused: everyone stops hearing. */
    public void paused() {
        WRITER.execute(() -> closeOpenSegments("pause"));
    }

    /** Player resumed: everyone in the channel starts hearing again. */
    public void resumed(List<UserRef> listeners) {
        WRITER.execute(() -> openSegments(listeners, "unpause"));
    }

    /** The bot got server-muted: everyone stops hearing. */
    public void botMuted() {
        WRITER.execute(() -> closeOpenSegments("bot_mute"));
    }

    /** The bot got unmuted: everyone in the channel hears again. */
    public void botUnmuted(List<UserRef> listeners) {
        WRITER.execute(() -> openSegments(listeners, "bot_unmute"));
    }

    /** The bot was moved to another channel: old listeners out, new ones in. */
    public void botMoved(List<UserRef> newListeners, boolean audible) {
        WRITER.execute(() -> {
            closeOpenSegments("bot_move");
            if (audible) {
                openSegments(newListeners, "bot_move");
            }
        });
    }

    /**
     * A user started hearing the play (joined/moved into the channel,
     * undeafened, ...).
     *
     * @param user The listener
     * @param reason A start reason from the listening_segments contract
     */
    public void userStartedListening(UserRef user, String reason) {
        WRITER.execute(() -> openSegments(List.of(user), reason));
    }

    /**
     * A user stopped hearing the play (left/moved out of the channel,
     * deafened, ...).
     *
     * @param userId The listener's id
     * @param reason A stop reason from the listening_segments contract
     */
    public void userStoppedListening(long userId, String reason) {
        WRITER.execute(() -> {

            if (this.playId == -1) { return; }

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE listening_segments SET ended_at = now(), end_reason = ? " +
                         "WHERE play_id = ? AND user_id = ? AND ended_at IS NULL")) {

                statement.setString(1, reason);
                statement.setLong(2, this.playId);
                statement.setLong(3, userId);
                statement.executeUpdate();

            } catch (Exception ignored) { }

        });
    }

    /* -------------------------------------------------------------------- */
    /*  Writer-thread internals                                             */
    /* -------------------------------------------------------------------- */

    private void openSegments(List<UserRef> listeners, String reason) {

        if (this.playId == -1 || listeners.isEmpty()) { return; }

        for (UserRef listener : listeners) {
            UserStore.upsert(listener);
        }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO listening_segments (play_id, user_id, start_reason) " +
                     "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM listening_segments " +
                     "WHERE play_id = ? AND user_id = ? AND ended_at IS NULL)")) {

            for (UserRef listener : listeners) {
                statement.setLong(1, this.playId);
                statement.setLong(2, listener.id());
                statement.setString(3, reason);
                statement.setLong(4, this.playId);
                statement.setLong(5, listener.id());
                statement.addBatch();
            }

            statement.executeBatch();

        } catch (Exception ignored) { }

    }

    private void closeOpenSegments(String reason) {

        if (this.playId == -1) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE listening_segments SET ended_at = now(), end_reason = ? " +
                     "WHERE play_id = ? AND ended_at IS NULL")) {

            statement.setString(1, reason);
            statement.setLong(2, this.playId);
            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    private void closePlay(String reason, Long playedMs) {

        if (this.playId == -1) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE track_plays SET ended_at = now(), played_ms = ?, end_reason = ? " +
                     "WHERE id = ? AND ended_at IS NULL")) {

            if (playedMs == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, playedMs);
            }
            statement.setString(2, reason);
            statement.setLong(3, this.playId);
            statement.executeUpdate();

        } catch (Exception ignored) { }

        this.playId = -1;

    }

    /* -------------------------------------------------------------------- */
    /*  Snapshots (caller-thread helpers)                                   */
    /* -------------------------------------------------------------------- */

    /**
     * Snapshots the users who can currently hear the bot: the non-bot,
     * non-deafened members of its voice channel. Taken on the caller (event)
     * thread so the writer thread never touches JDA entities.
     *
     * @param guild The guild to snapshot
     * @return The eligible listeners (possibly empty, never null)
     */
    public static List<UserRef> snapshotListeners(Guild guild) {

        List<UserRef> listeners = new ArrayList<>();

        try {

            AudioChannel channel = guild.getAudioManager().getConnectedChannel();
            if (channel == null) { return listeners; }

            for (Member member : channel.getMembers()) {
                if (member.getUser().isBot()) { continue; }

                GuildVoiceState voiceState = member.getVoiceState();
                if (voiceState != null && (voiceState.isDeafened() || voiceState.isSelfDeafened())) { continue; }

                listeners.add(UserRef.of(member));
            }

        } catch (Exception ignored) { }

        return listeners;

    }

    /**
     * Whether the bot itself is currently audible in this guild (connected and
     * not server-muted). Combined with "playing and not paused" by callers.
     *
     * @param guild The guild to check
     * @return true when nothing on the bot's side blocks the audio
     */
    public static boolean isBotAudible(Guild guild) {

        try {
            GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
            return voiceState == null || !voiceState.isGuildMuted();
        } catch (Exception ignored) {
            return true;
        }

    }

}
