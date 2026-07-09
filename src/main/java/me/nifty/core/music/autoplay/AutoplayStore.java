package me.nifty.core.music.autoplay;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.TrackStore;
import me.nifty.managers.DatabaseManager;
import me.nifty.utils.TrackUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * The autoplay recommendation buffer for this (bot, guild) pair: ordered
 * {@code autoplay_tracks} rows referencing the shared {@code tracks} catalog.
 *
 * <p>Same position contract as the queue — contiguous, starting at 0. Every
 * structural change renumbers the remaining rows inside its transaction, so
 * positions can never gap. The buffer is small (≤ {@code AutoplayManager.MAX_SIZE})
 * which keeps the renumbering trivial.</p>
 */
public class AutoplayStore {

    private final long guildId;

    public AutoplayStore(long guildId) {
        this.guildId = guildId;
    }

    /** One buffer row, with its catalog track decoded and ready to play. */
    public record Entry(long id, long trackId, AudioTrack track, String provider) { }

    /**
     * Gets the number of tracks currently in the buffer.
     *
     * @return The buffer size.
     */
    public int size() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM autoplay_tracks WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            ResultSet result = statement.executeQuery();
            if (result.next()) { return result.getInt(1); }

        } catch (Exception ignored) { }

        return 0;

    }

    /**
     * Appends a recommended track to the end of the buffer.
     *
     * @param track The recommended track.
     * @param provider The recommender that produced it ('deezer', 'youtube', ...).
     * @param seedTrackId The catalog id of the queue track it derives from.
     * @return true if the row was inserted.
     */
    public boolean append(AudioTrack track, String provider, Long seedTrackId) {

        long trackId = TrackStore.upsert(track);
        if (trackId == -1) { return false; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO autoplay_tracks (bot_id, guild_id, position, track_id, provider, seed_track_id) " +
                     "SELECT ?, ?, COALESCE(MAX(position) + 1, 0), ?, ?, ? FROM autoplay_tracks " +
                     "WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);
            statement.setLong(3, trackId);
            statement.setString(4, provider);
            if (seedTrackId == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, seedTrackId);
            }
            statement.setLong(6, BotIdentity.get());
            statement.setLong(7, this.guildId);

            return statement.executeUpdate() > 0;

        } catch (Exception ignored) { }

        return false;

    }

    /**
     * Removes and returns the head of the buffer (the next track autoplay
     * should feed into the queue), or null when the buffer is empty.
     *
     * @return The consumed entry, with its track decoded (track may be null
     *         if the cached blob no longer decodes — callers skip those).
     */
    public Entry takeFirst() {
        return take(-1);
    }

    /**
     * Removes and returns a specific buffer row by id (dashboard remove /
     * promote), or the head row when {@code id} is -1. Remaining rows are
     * renumbered in the same transaction.
     *
     * @param id The autoplay_tracks row id, or -1 for the head.
     * @return The removed entry, or null if it no longer exists.
     */
    public Entry take(long id) {

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                Entry entry = null;

                String select = id == -1
                        ? "SELECT at.id, at.track_id, at.provider, t.encoded FROM autoplay_tracks at " +
                          "JOIN tracks t ON t.id = at.track_id " +
                          "WHERE at.bot_id = ? AND at.guild_id = ? ORDER BY at.position ASC LIMIT 1"
                        : "SELECT at.id, at.track_id, at.provider, t.encoded FROM autoplay_tracks at " +
                          "JOIN tracks t ON t.id = at.track_id " +
                          "WHERE at.bot_id = ? AND at.guild_id = ? AND at.id = ?";

                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setLong(1, BotIdentity.get());
                    statement.setLong(2, this.guildId);
                    if (id != -1) { statement.setLong(3, id); }

                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        AudioTrack track = TrackUtils.decodeTrack(result.getString("encoded"));
                        entry = new Entry(result.getLong("id"), result.getLong("track_id"), track, result.getString("provider"));
                    }
                }

                if (entry == null) {
                    connection.rollback();
                    return null;
                }

                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM autoplay_tracks WHERE id = ?")) {
                    delete.setLong(1, entry.id());
                    delete.executeUpdate();
                }

                compact(connection);

                connection.commit();
                return entry;

            } catch (Exception e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception ignored) { }

        return null;

    }

    /**
     * Moves a buffer row to a new index (dashboard drag-reorder). The whole
     * buffer is renumbered around it — at ≤ 20 rows that is the simplest
     * correct thing.
     *
     * @param id The autoplay_tracks row id.
     * @param toIndex The target index within the buffer.
     */
    public void move(long id, int toIndex) {

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                List<Long> rowIds = new ArrayList<>();

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id FROM autoplay_tracks WHERE bot_id = ? AND guild_id = ? ORDER BY position ASC")) {
                    select.setLong(1, BotIdentity.get());
                    select.setLong(2, this.guildId);

                    ResultSet result = select.executeQuery();
                    while (result.next()) { rowIds.add(result.getLong("id")); }
                }

                if (!rowIds.remove(Long.valueOf(id))) {
                    connection.rollback();
                    return;
                }

                int dest = Math.max(0, Math.min(toIndex, rowIds.size()));
                rowIds.add(dest, id);

                try (PreparedStatement place = connection.prepareStatement(
                        "UPDATE autoplay_tracks SET position = ? WHERE id = ?")) {

                    for (int i = 0; i < rowIds.size(); i++) {
                        place.setInt(1, i);
                        place.setLong(2, rowIds.get(i));
                        place.addBatch();
                    }

                    place.executeBatch();
                }

                connection.commit();

            } catch (Exception e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception ignored) { }

    }

    /**
     * Deletes buffer rows whose track meanwhile landed in the real queue
     * (a user queued the same song), so the buffer never advertises something
     * already coming up.
     *
     * @return How many rows were pruned.
     */
    public int removeQueueDuplicates() {

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int pruned;

                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM autoplay_tracks at USING queue_tracks qt " +
                        "WHERE at.bot_id = ? AND at.guild_id = ? " +
                        "AND qt.bot_id = at.bot_id AND qt.guild_id = at.guild_id AND qt.track_id = at.track_id")) {
                    delete.setLong(1, BotIdentity.get());
                    delete.setLong(2, this.guildId);
                    pruned = delete.executeUpdate();
                }

                if (pruned > 0) {
                    compact(connection);
                }

                connection.commit();
                return pruned;

            } catch (Exception e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception ignored) { }

        return 0;

    }

    /**
     * Empties the buffer (autoplay disabled / player destroyed).
     */
    public void clear() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM autoplay_tracks WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    /**
     * Records that a user removed a recommendation from the buffer — negative
     * feedback the recommender uses to exclude the track going forward.
     *
     * @param trackId The catalog id of the removed track.
     * @param userId The removing user's id (0/unknown stores NULL).
     */
    public void recordRemoval(long trackId, long userId) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO autoplay_feedback (guild_id, track_id, user_id, kind) VALUES (?, ?, ?, 'removed')")) {

            statement.setLong(1, this.guildId);
            statement.setLong(2, trackId);
            if (userId == 0) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, userId);
            }

            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    /**
     * Renumbers the buffer 0..n-1 in position order. Runs inside the caller's
     * transaction (the deferred unique constraint makes the shuffle atomic).
     */
    private void compact(Connection connection) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE autoplay_tracks at SET position = numbered.new_position - 1 " +
                "FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY position ASC) AS new_position " +
                "      FROM autoplay_tracks WHERE bot_id = ? AND guild_id = ?) numbered " +
                "WHERE at.id = numbered.id AND at.position <> numbered.new_position - 1")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);
            statement.executeUpdate();
        }

    }

}
