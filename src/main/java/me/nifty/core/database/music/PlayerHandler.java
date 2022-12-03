package me.nifty.core.database.music;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import me.nifty.managers.DatabaseManager;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;

import java.sql.*;

public class PlayerHandler {

    private final long guildId;

    private long textChannelId;
    private long voiceChannelId;

    private int position;

    private Loop loop = Loop.DISABLED;
    private Autoplay autoplay = Autoplay.DISABLED;
    private Shuffle shuffle = Shuffle.DISABLED;

    private float speed = 1.0f;
    private float pitch = 1.0f;
    private float bassBoost = 0.0f;
    private boolean rotation = false;

    public PlayerHandler(long guildId) {
        this.guildId = guildId;

        boolean playerCreated = create();

        if (!playerCreated) {
            throw new FriendlyException("Failed to create a player in the database!", FriendlyException.Severity.SUSPICIOUS, null);
        }

    }

    /**
     * Creates a player on the database.
     * @return true if the player was created successfully, false otherwise.
     */
    private boolean create() {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO Players (guild_id, position, playing) VALUES (?, ?, ?) ON CONFLICT DO UPDATE SET guild_id = ?");
            insertStatement.setLong(1, this.guildId);
            insertStatement.setInt(2, 0);
            insertStatement.setBoolean(3, false);
            insertStatement.setLong(4, this.guildId);

            int insertResult = insertStatement.executeUpdate();

            if (insertResult == 1) {
                return true;
            }

            if (insertResult == 2) {
                reload();
                return true;
            }

            return false;

        } catch (Exception ignored) { }

        return false;

    }

    /**
     * Reloads the player from the database into the cache.
     */
    public void reload() {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Players WHERE guild_id = ?");
            selectStatement.setLong(1, this.guildId);

            ResultSet result = selectStatement.executeQuery();

            if (result.next()) {
                this.textChannelId = result.getLong("channel_id");
                this.voiceChannelId = result.getLong("voice_id");
                this.position = result.getInt("position");

                this.autoplay = result.getString("autoplay") == null ? Autoplay.DISABLED : Autoplay.valueOf(result.getString("autoplay"));
                this.loop = result.getString("loop") == null ? Loop.DISABLED : Loop.valueOf(result.getString("loop"));
                this.shuffle = result.getString("shuffle") == null ? Shuffle.DISABLED : Shuffle.valueOf(result.getString("shuffle"));

                this.speed = result.getFloat("speed") == 0.0f ? 1.0f : result.getFloat("speed");
                this.pitch = result.getFloat("pitch") == 0.0f ? 1.0f : result.getFloat("pitch");
                this.bassBoost = result.getFloat("bass_boost");
                this.rotation = result.getBoolean("rotation");
            }

        } catch (Exception ignored) { }


    }

    /**
     * Deletes the player from the database.
     */
    public void delete() {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM Players WHERE guild_id = ?");
            deleteStatement.setLong(1, this.guildId);

            deleteStatement.executeUpdate();

        } catch (Exception ignored) { }

    }

    /**
     * Gets the current position of the player.
     *
     * @return The current position of the player.
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Sets the current position of the player.
     *
     * @param position The new position of the player.
     */
    public void setPosition(int position) {

        Connection connection = DatabaseManager.getConnection();

        this.position = position;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET position = ? WHERE guild_id = ?");
            updateStatement.setInt(1, position);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Sets if the player is currently playing a track or not.
     *
     * @param playing Whether the player is currently playing or not.
     */
    public void setPlaying(boolean playing) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET playing = ? WHERE guild_id = ?");
            updateStatement.setBoolean(1, playing);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the text channel id of the player.
     *
     * @return The text channel id of the player.
     */
    public long getTextChannelId() {
        return this.textChannelId;
    }

    /**
     * Sets the text channel id of the player.
     *
     * @param textChannelId The new text channel id of the player.
     */
    public void setTextChannelId(long textChannelId) {

        Connection connection = DatabaseManager.getConnection();

        this.textChannelId = textChannelId;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET channel_id = ? WHERE guild_id = ?");
            updateStatement.setLong(1, textChannelId);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the voice channel id of the player.
     *
     * @return The voice channel id of the player.
     */
    public long getVoiceChannelId() {
        return this.voiceChannelId;
    }

    /**
     * Sets the voice channel id of the player.
     *
     * @param voiceChannelId The new voice channel id of the player.
     */
    public void setVoiceChannelId(long voiceChannelId) {

        Connection connection = DatabaseManager.getConnection();

        this.voiceChannelId = voiceChannelId;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET voice_id = ? WHERE guild_id = ?");
            updateStatement.setLong(1, voiceChannelId);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the autoplay mode for the player.
     *
     * @return The autoplay mode of the player.
     */
    public Autoplay getAutoplayMode() {
        return this.autoplay;
    }

    /**
     * Sets the autoplay mode for the player.
     *
     * @param autoplayMode The new autoplay mode of the player.
     */
    public void setAutoplayMode(Autoplay autoplayMode) {

        Connection connection = DatabaseManager.getConnection();

        this.autoplay = autoplayMode;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET autoplay = ? WHERE guild_id = ?");
            updateStatement.setString(1, autoplayMode.name());
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the loop mode of the player.
     *
     * @return The loop
     */
    public Loop getLoopMode() {
        return this.loop;
    }

    /**
     * Sets the loop mode of the player.
     *
     * @param loopMode The loop mode to set.
     */
    public void setLoopMode(Loop loopMode) {

        Connection connection = DatabaseManager.getConnection();

        this.loop = loopMode;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET looping = ? WHERE guild_id = ?");
            updateStatement.setString(1, loopMode.name());
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the shuffle mode of the player.
     *
     * @return The shuffle mode.
     */
    public Shuffle getShuffleMode() {
        return this.shuffle;
    }

    /**
     * Sets the shuffle mode of the player.
     *
     * @param shuffleMode The shuffle mode to set.
     */
    public void setShuffleMode(Shuffle shuffleMode) {

        Connection connection = DatabaseManager.getConnection();

        this.shuffle = shuffleMode;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET shuffle = ? WHERE guild_id = ?");
            updateStatement.setString(1, shuffleMode.name());
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the speed of the player.
     *
     * @return The speed of the player.
     */
    public float getSpeed() {
        return this.speed;
    }

    /**
     * Sets the speed of the player.
     *
     * @param speed The new speed of the player.
     */
    public void setSpeed(float speed) {

        Connection connection = DatabaseManager.getConnection();

        this.speed = speed;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET speed = ? WHERE guild_id = ?");
            updateStatement.setFloat(1, speed);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the pitch of the player.
     *
     * @return The pitch of the player.
     */
    public float getPitch() {
        return this.pitch;
    }

    /**
     * Sets the pitch of the player.
     *
     * @param pitch The new pitch of the player.
     */
    public void setPitch(float pitch) {

        Connection connection = DatabaseManager.getConnection();

        this.pitch = pitch;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET pitch = ? WHERE guild_id = ?");
            updateStatement.setFloat(1, pitch);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) {}

    }

    /**
     * Gets the bass boost of the player.
     *
     * @return The bass boost of the player.
     */
    public float getBassBoost() {
        return this.bassBoost;
    }

    /**
     * Sets the bass boost of the player.
     *
     * @param bassBoost The new bass boost of the player.
     */
    public void setBassBoost(float bassBoost) {

        Connection connection = DatabaseManager.getConnection();

        this.bassBoost = bassBoost;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET bass_boost = ? WHERE guild_id = ?");
            updateStatement.setFloat(1, bassBoost);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

    /**
     * Gets the rotation of the player.
     *
     * @return The rotation of the player.
     */
    public boolean getRotation() {
        return this.rotation;
    }

    /**
     * Sets the rotation of the player.
     *
     * @param rotation The new rotation of the player.
     */
    public void setRotation(boolean rotation) {

        Connection connection = DatabaseManager.getConnection();

        this.rotation = rotation;

        try {

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Players SET rotation = ? WHERE guild_id = ?");
            updateStatement.setBoolean(1, rotation);
            updateStatement.setLong(2, guildId);

            updateStatement.executeUpdate();

        } catch (SQLException ignored) { }

    }

}
