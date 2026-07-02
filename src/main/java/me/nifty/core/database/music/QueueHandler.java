package me.nifty.core.database.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.GuildStore;
import me.nifty.core.database.TrackStore;
import me.nifty.core.database.UserStore;
import me.nifty.managers.DatabaseManager;
import me.nifty.utils.TrackUtils;
import me.nifty.websocket.payloads.WsUpdates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The live queue for this (bot, guild) pair: ordered {@code queue_tracks}
 * rows referencing the shared {@code tracks} catalog.
 *
 * <p>Positions are contiguous, starting at 0. Every reorder runs inside a
 * transaction — the deferrable unique constraint on (bot_id, guild_id,
 * position) keeps shifts atomic and consistent.</p>
 */
public class QueueHandler {

    private final long guildId;

    public QueueHandler(long guildId) {
        this.guildId = guildId;
        GuildStore.ensure(guildId);
    }

    /**
     * Adds a track to the queue.
     *
     * @param audioTrack The track to add.
     * @param position The position to add the track to.
     */
    public void addTrack(AudioTrack audioTrack, int position) {
        addTracks(Collections.singletonList(audioTrack), position);
    }

    /**
     * Adds multiple tracks to the queue in one transaction.
     *
     * @param audioTracks The tracks to add.
     * @param position The position to add the tracks at.
     */
    public void addTracks(List<AudioTrack> audioTracks, int position) {

        if (audioTracks == null || audioTracks.isEmpty()) { return; }

        // Resolve catalog ids and make sure the queuing users exist before the
        // queue rows reference them (both have their own connections).
        List<Long> trackIds = new ArrayList<>(audioTracks.size());
        List<Long> queuedBy = new ArrayList<>(audioTracks.size());

        for (AudioTrack audioTrack : audioTracks) {
            long trackId = TrackStore.upsert(audioTrack);
            if (trackId == -1) { continue; }

            long memberId = audioTrack.getUserData() instanceof Long ? (Long) audioTrack.getUserData() : 0L;
            UserStore.ensureExists(memberId);

            trackIds.add(trackId);
            queuedBy.add(memberId);
        }

        if (trackIds.isEmpty()) { return; }

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement shift = connection.prepareStatement(
                        "UPDATE queue_tracks SET position = position + ? WHERE bot_id = ? AND guild_id = ? AND position >= ?")) {
                    shift.setInt(1, trackIds.size());
                    shift.setLong(2, BotIdentity.get());
                    shift.setLong(3, this.guildId);
                    shift.setInt(4, position);
                    shift.executeUpdate();
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO queue_tracks (bot_id, guild_id, position, track_id, queued_by) VALUES (?, ?, ?, ?, ?)")) {

                    for (int i = 0; i < trackIds.size(); i++) {
                        insert.setLong(1, BotIdentity.get());
                        insert.setLong(2, this.guildId);
                        insert.setInt(3, position + i);
                        insert.setLong(4, trackIds.get(i));
                        insert.setLong(5, queuedBy.get(i));
                        insert.addBatch();
                    }

                    insert.executeBatch();
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
     * Gets the track at the specified position.
     *
     * @param position The position of the track.
     * @return The track at the specified position.
     */
    public AudioTrack getQueueTrack(int position) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT t.encoded, qt.queued_by FROM queue_tracks qt " +
                     "JOIN tracks t ON t.id = qt.track_id " +
                     "WHERE qt.bot_id = ? AND qt.guild_id = ? AND qt.position = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);
            statement.setInt(3, position);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                AudioTrack audioTrack = TrackUtils.decodeTrack(result.getString("encoded"));
                if (audioTrack == null) { return null; }

                audioTrack.setUserData(result.getLong("queued_by"));

                return audioTrack;
            }

        } catch (Exception ignored) { }

        return null;

    }

    /**
     * Gets the track that matches the given query.
     *
     * @return The position of the track, or -1 if no track was found.
     */
    public int searchQueueTrack(String query) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT qt.position FROM queue_tracks qt " +
                     "JOIN tracks t ON t.id = qt.track_id " +
                     "WHERE qt.bot_id = ? AND qt.guild_id = ? AND LOWER(t.artist || ' - ' || t.title) LIKE ? " +
                     "ORDER BY qt.position ASC LIMIT 1")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);
            statement.setString(3, "%" + query.toLowerCase() + "%");

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getInt("position");
            }

        } catch (Exception ignored) { }

        return -1;

    }

    /**
     * Gets a list of tracks from the queue.
     *
     * @param page The page of the queue to get.
     * @return The list of tracks from the queue.
     */
    public List<AudioTrack> getQueuePage(int page) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT t.encoded, qt.queued_by FROM queue_tracks qt " +
                     "JOIN tracks t ON t.id = qt.track_id " +
                     "WHERE qt.bot_id = ? AND qt.guild_id = ? " +
                     "ORDER BY qt.position ASC LIMIT 10 OFFSET ?")) {

            int offset = page * 10;

            int queueSize = getQueueSize();
            int pageMax = queueSize / 10;

            if (queueSize < 10) {
                offset = 0;
            } else if (page == pageMax) {
                offset = -(10 - queueSize);
            }

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);
            statement.setInt(3, Math.max(0, offset));

            ResultSet result = statement.executeQuery();

            List<AudioTrack> audioTracks = new ArrayList<>();

            while (result.next()) {
                AudioTrack audioTrack = TrackUtils.decodeTrack(result.getString("encoded"));
                if (audioTrack == null) { continue; }

                audioTrack.setUserData(result.getLong("queued_by"));

                audioTracks.add(audioTrack);
            }

            if (!audioTracks.isEmpty()) {
                return audioTracks;
            }

        } catch (Exception ignored) { }

        return null;

    }

    /**
     * Gets the size of the queue.
     *
     * @return The size of the queue.
     */
    public int getQueueSize() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM queue_tracks WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getInt(1);
            }

        } catch (Exception ignored) { }

        return 0;

    }

    /**
     * Moves a track in the queue: the rows between the two positions shift by
     * one and the moved row lands on the target — all in one transaction.
     *
     * @param position The position of the track to move.
     * @param newPosition The new position of the track.
     */
    public void moveTrack(int position, int newPosition) {

        if (position == newPosition) { return; }

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                long movedId = -1;

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id FROM queue_tracks WHERE bot_id = ? AND guild_id = ? AND position = ?")) {
                    select.setLong(1, BotIdentity.get());
                    select.setLong(2, this.guildId);
                    select.setInt(3, position);

                    ResultSet result = select.executeQuery();
                    if (result.next()) {
                        movedId = result.getLong("id");
                    }
                }

                if (movedId == -1) {
                    connection.rollback();
                    return;
                }

                String shiftSql = newPosition > position
                        ? "UPDATE queue_tracks SET position = position - 1 WHERE bot_id = ? AND guild_id = ? AND position > ? AND position <= ?"
                        : "UPDATE queue_tracks SET position = position + 1 WHERE bot_id = ? AND guild_id = ? AND position >= ? AND position < ?";

                try (PreparedStatement shift = connection.prepareStatement(shiftSql)) {
                    shift.setLong(1, BotIdentity.get());
                    shift.setLong(2, this.guildId);
                    if (newPosition > position) {
                        shift.setInt(3, position);
                        shift.setInt(4, newPosition);
                    } else {
                        shift.setInt(3, newPosition);
                        shift.setInt(4, position);
                    }
                    shift.executeUpdate();
                }

                try (PreparedStatement place = connection.prepareStatement(
                        "UPDATE queue_tracks SET position = ? WHERE id = ?")) {
                    place.setInt(1, newPosition);
                    place.setLong(2, movedId);
                    place.executeUpdate();
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
     * Moves multiples tracks in the queue.
     *
     * @param position The position starting position of the tracks to move.
     * @param newPosition The new position to move the tracks to.
     * @param amount The amount of tracks to move.
     */
    public void moveTracks(int position, int newPosition, int amount) {

        int endPosition = position + amount;

        if (newPosition >= position && newPosition <= endPosition) { return; }

        for (int i = 0; i < amount; i++) {

            moveTrack(position, newPosition);

            if (position > newPosition) {
                position++;
                newPosition++;
            }

        }

    }

    /**
     * Removes a track from the queue.
     *
     * @param position The position of the track to remove.
     */
    public void removeTrack(int position) {

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int deleted;

                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM queue_tracks WHERE bot_id = ? AND guild_id = ? AND position = ?")) {
                    delete.setLong(1, BotIdentity.get());
                    delete.setLong(2, this.guildId);
                    delete.setInt(3, position);
                    deleted = delete.executeUpdate();
                }

                if (deleted > 0) {
                    try (PreparedStatement shift = connection.prepareStatement(
                            "UPDATE queue_tracks SET position = position - 1 WHERE bot_id = ? AND guild_id = ? AND position > ?")) {
                        shift.setLong(1, BotIdentity.get());
                        shift.setLong(2, this.guildId);
                        shift.setInt(3, position);
                        shift.executeUpdate();
                    }
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
     * Removes multiple tracks from the queue from a starting position.
     *
     * @param position The starting position of the tracks to remove.
     * @param amount The amount of tracks to remove.
     */
    public void removeTracks(int position, int amount) {

        for (int i = 0; i < amount; i++) {
            removeTrack(position);
        }

    }

    /**
     * Shuffles the tracks in the queue after the given position.
     *
     * @param startPosition The starting position of the tracks to shuffle.
     */
    public void shuffleAfter(int startPosition) {

        try (Connection connection = DatabaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                List<Long> rowIds = new ArrayList<>();

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id FROM queue_tracks WHERE bot_id = ? AND guild_id = ? AND position >= ? ORDER BY position ASC")) {
                    select.setLong(1, BotIdentity.get());
                    select.setLong(2, this.guildId);
                    select.setInt(3, startPosition);

                    ResultSet result = select.executeQuery();
                    while (result.next()) {
                        rowIds.add(result.getLong("id"));
                    }
                }

                if (rowIds.isEmpty()) {
                    connection.rollback();
                } else {

                    Collections.shuffle(rowIds);

                    try (PreparedStatement place = connection.prepareStatement(
                            "UPDATE queue_tracks SET position = ? WHERE id = ?")) {

                        for (int i = 0; i < rowIds.size(); i++) {
                            place.setInt(1, startPosition + i);
                            place.setLong(2, rowIds.get(i));
                            place.addBatch();
                        }

                        place.executeBatch();
                    }

                    connection.commit();

                }

            } catch (Exception e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception ignored) { }

        // Push the reshuffled queue to the dashboard once (no-op if disconnected)
        WsUpdates.queue(this.guildId);

    }

    /**
     * Clears the queue.
     */
    public void clearQueue() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM queue_tracks WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            statement.executeUpdate();

        } catch (Exception ignored) { }

        // Push the now-empty queue to the dashboard (no-op if disconnected)
        WsUpdates.queue(this.guildId);

    }

}
