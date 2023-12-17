package me.nifty.core.database.music;

import com.github.topisenpai.lavasrc.deezer.DeezerAudioTrack;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.DatabaseManager;
import me.nifty.managers.JDAManager;
import me.nifty.utils.TrackUtils;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.utils.formatting.WsQueue;
import net.dv8tion.jda.api.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.nifty.managers.JDAManager.getJDA;

public class QueueHandler {

    private final long guildId;

    public QueueHandler(long guildId) {
        this.guildId = guildId;
    }

    /**
     * Adds a track to the queue.
     *
     * @param audioTrack The track to add.
     * @param position The position to add the track to.
     */
    public void addTrack(AudioTrack audioTrack, int position, boolean ignoreWs) {

        Connection connection = DatabaseManager.getConnection();

        try {

            String trackTitle = TrackTitle.format(audioTrack, 255);

            // Inserts the track into the database
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO Queues (guild_id, track_id, track_name, track_url, artwork_url, duration, member_avatar_url, member_name, member_discrim, member_id, encoded_track) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            insertStatement.setLong(1, this.guildId);
            insertStatement.setInt(2, position);
            insertStatement.setString(3, trackTitle);
            insertStatement.setString(4, audioTrack.getInfo().uri);

            if (audioTrack.getSourceManager().getSourceName().equals("youtube")) {
                insertStatement.setString(5, "https://i.ytimg.com/vi/" + audioTrack.getInfo().identifier + "/mqdefault.jpg");
            } else if (audioTrack instanceof MirroringAudioTrack) {
                insertStatement.setString(5, ((MirroringAudioTrack) audioTrack).getArtworkURL());
            } else if (audioTrack instanceof DeezerAudioTrack) {
                insertStatement.setString(5, ((DeezerAudioTrack) audioTrack).getArtworkURL());
            } else {
                insertStatement.setString(5, null);
            }

            insertStatement.setLong(6, audioTrack.getDuration());

            User user = JDAManager.getJDA().getUserById(audioTrack.getUserData().toString());
            if (user == null) { return; }

            insertStatement.setString(7, user.getEffectiveAvatarUrl());
            insertStatement.setString(8, user.getName());
            insertStatement.setString(9, user.getDiscriminator());

            insertStatement.setLong(10, (long) audioTrack.getUserData());
            insertStatement.setString(11, TrackUtils.encodeTrack(audioTrack));

            int insertResult = insertStatement.executeUpdate();
            if (insertResult == 0) { return; }

            ResultSet generatedKeys = insertStatement.getGeneratedKeys();
            if (!generatedKeys.next()) { return; }

            int trackId = generatedKeys.getInt(1);

            // Updates the position of the tracks after the inserted track
            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Queues SET track_id = track_id + 1 WHERE guild_id = ? AND id != ? AND track_id > ?");
            updateStatement.setLong(1, this.guildId);
            updateStatement.setInt(2, trackId);
            updateStatement.setInt(3, (position - 1));

            updateStatement.executeUpdate();

        } catch (Exception ignored) { }

        if (!ignoreWs) {
            WsQueue.updateWsQueue(this.guildId);
        }

    }

    /**
     * Adds multiple tracks to the queue.
     *
     * @param audioTracks The tracks to add.
     * @param position The position to add the tracks to.
     */
    public void addTracks(List<AudioTrack> audioTracks, int position) {

        for (AudioTrack audioTrack : audioTracks) {
            addTrack(audioTrack, position, true);
            position++;
        }

        WsQueue.updateWsQueue(this.guildId);

    }

    /**
     * Gets the track at the specified position.
     *
     * @param position The position of the track.
     * @return The track at the specified position.
     */
    public AudioTrack getQueueTrack(int position) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Queues WHERE guild_id = ? AND track_id = ?");
            selectStatement.setLong(1, this.guildId);
            selectStatement.setInt(2, position);

            ResultSet selectResult = selectStatement.executeQuery();

            if (selectResult.next()) {
                AudioTrack audioTrack = TrackUtils.decodeTrack(selectResult.getString("encoded_track"));
                if (audioTrack == null) { return null; }

                audioTrack.setUserData(selectResult.getLong("member_id"));

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

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Queues WHERE guild_id = ? AND LOWER(track_name) LIKE ?");
            selectStatement.setLong(1, this.guildId);
            selectStatement.setString(2, "%" + query.toLowerCase() + "%");

            ResultSet selectResult = selectStatement.executeQuery();

            if (selectResult.next()) {
                return selectResult.getInt("track_id");
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

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Queues WHERE guild_id = ? ORDER BY track_id ASC LIMIT 10 OFFSET ?");
            selectStatement.setLong(1, this.guildId);

            int offset = page * 10;

            int queueSize = getQueueSize();
            int pageMax = queueSize / 10;

            if (queueSize < 10) {
                offset = 0;
            } else if (page == pageMax) {
                offset = -(10 - queueSize) ;
            }

            selectStatement.setInt(2, offset);

            ResultSet selectResult = selectStatement.executeQuery();

            List<AudioTrack> audioTracks = new ArrayList<>();

            while (selectResult.next()) {
                AudioTrack audioTrack = TrackUtils.decodeTrack(selectResult.getString("encoded_track"));
                if (audioTrack == null) { continue; }

                audioTrack.setUserData(selectResult.getLong("member_id"));

                audioTracks.add(audioTrack);
            }

            if (audioTracks.size() > 0) {
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

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT COUNT(*) FROM Queues WHERE guild_id = ?");
            selectStatement.setLong(1, this.guildId);

            ResultSet selectResult = selectStatement.executeQuery();

            if (selectResult.next()) {
                return selectResult.getInt(1);
            }

        } catch (Exception ignored) { }

        return 0;

    }

    /**
     * Moves a track in the queue.
     *
     * @param position The position of the track to move.
     * @param newPosition The new position of the track.
     */
    public void moveTrack(int position, int newPosition, boolean ignoreWs) {

        if (position == newPosition) { return; }

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Queues WHERE guild_id = ? AND track_id = ?");
            selectStatement.setLong(1, this.guildId);
            selectStatement.setInt(2, position);

            ResultSet selectResult = selectStatement.executeQuery();
            if (!selectResult.next()) { return; }

            AudioTrack audioTrack = TrackUtils.decodeTrack(selectResult.getString("encoded_track"));
            if (audioTrack == null) { return; }

            audioTrack.setUserData(selectResult.getLong("member_id"));

            removeTrack(position, true);
            addTrack(audioTrack, newPosition, true);

        } catch (Exception ignored) { }

        if (!ignoreWs) {
            WsQueue.updateWsQueue(this.guildId);
        }

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

            moveTrack(position, newPosition, true);

            if (position > newPosition) {
                position++;
                newPosition++;
            }

        }

        WsQueue.updateWsQueue(this.guildId);

    }

    /**
     * Removes a track from the queue.
     *
     * @param position The position of the track to remove.
     */
    public void removeTrack(int position, boolean ignoreWs) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM Queues WHERE guild_id = ? AND track_id = ?");
            deleteStatement.setLong(1, this.guildId);
            deleteStatement.setInt(2, position);

            int deleteResult = deleteStatement.executeUpdate();
            if (deleteResult == 0) { return; }

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Queues SET track_id = track_id - 1 WHERE guild_id = ? AND track_id > ?");
            updateStatement.setLong(1, this.guildId);
            updateStatement.setInt(2, position);

            updateStatement.executeUpdate();

        } catch (Exception ignored) { }

        if (!ignoreWs) {
            WsQueue.updateWsQueue(this.guildId);
        }

    }

    /**
     * Removes multiple tracks from the queue from a starting position.
     *
     * @param position The starting position of the tracks to remove.
     * @param amount The amount of tracks to remove.
     */
    public void removeTracks(int position, int amount) {

        for (int i = 0; i < amount; i++) {
            removeTrack(position, true);
        }

        WsQueue.updateWsQueue(this.guildId);

    }

    /**
     * Shuffles the tracks in the queue after the given position.
     *
     * @param startPosition The starting position of the tracks to shuffle.
     */
    public void shuffleAfter(int startPosition) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Queues WHERE guild_id = ? AND track_id >= ? ORDER BY track_id ASC");
            selectStatement.setLong(1, this.guildId);
            selectStatement.setInt(2, startPosition);

            ResultSet selectResult = selectStatement.executeQuery();

            List<AudioTrack> audioTracks = new ArrayList<>();

            while (selectResult.next()) {
                AudioTrack audioTrack = TrackUtils.decodeTrack(selectResult.getString("encoded_track"));
                if (audioTrack == null) { continue; }

                audioTrack.setUserData(selectResult.getLong("member_id"));

                audioTracks.add(audioTrack);
            }

            if (audioTracks.size() == 0) { return; }

            Collections.shuffle(audioTracks);

            int position = startPosition;

            for (AudioTrack audioTrack : audioTracks) {
                removeTrack(position, true);
                addTrack(audioTrack, position, true);
                position++;
            }

        } catch (Exception ignored) { }

        WsQueue.updateWsQueue(this.guildId);

    }

    /**
     * Clears the queue.
     */
    public void clearQueue() {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM Queues WHERE guild_id = ?");
            deleteStatement.setLong(1, this.guildId);

            deleteStatement.executeUpdate();

        } catch (Exception ignored) { }

        WsQueue.updateWsQueue(this.guildId);

    }

}
