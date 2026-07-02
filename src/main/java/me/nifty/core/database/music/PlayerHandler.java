package me.nifty.core.database.music;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.GuildStore;
import me.nifty.managers.DatabaseManager;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * The live player row for this (bot, guild) pair in the shared {@code players}
 * table. Settings are cached in memory and written through on change.
 *
 * <p>Playback progress is a wall-clock anchor: {@code position_ms} plus
 * {@code position_at} are written only on events (play/pause/seek/track
 * change), never on a timer. Readers derive the current position as
 * {@code position_ms + (playing ? now() - position_at : 0)}.</p>
 */
public class PlayerHandler {

    private final long guildId;

    private long textChannelId;

    private int position;

    private Loop loop = Loop.DISABLED;
    private Autoplay autoplay = Autoplay.DISABLED;
    private Shuffle shuffle = Shuffle.DISABLED;

    private int volume = 100;

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

        GuildStore.ensure(this.guildId);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO players (bot_id, guild_id, queue_position, playing) VALUES (?, ?, 0, FALSE) " +
                     "ON CONFLICT (bot_id, guild_id) DO NOTHING")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            int insertResult = statement.executeUpdate();

            // An existing row survives restarts — load its state back.
            if (insertResult == 0) {
                reload();
            }

            return true;

        } catch (Exception ignored) { }

        return false;

    }

    /**
     * Reloads the player from the database into the cache.
     */
    public void reload() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM players WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                this.textChannelId = result.getLong("text_channel_id");
                this.position = result.getInt("queue_position");

                this.autoplay = Autoplay.valueOf(result.getString("autoplay").toUpperCase());
                this.loop = Loop.valueOf(result.getString("loop_mode").toUpperCase());
                this.shuffle = Shuffle.valueOf(result.getString("shuffle").toUpperCase());

                this.volume = result.getInt("volume");

                this.speed = result.getFloat("speed") == 0 ? 1.0f : result.getFloat("speed");
                this.pitch = result.getFloat("pitch") == 0 ? 1.0f : result.getFloat("pitch");
                this.bassBoost = result.getFloat("bass_boost");
                this.rotation = result.getBoolean("rotation");
            }

        } catch (Exception ignored) { }

    }

    /**
     * Deletes the player from the database.
     */
    public void delete() {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM players WHERE bot_id = ? AND guild_id = ?")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, this.guildId);

            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    /**
     * Runs a single-column UPDATE against this player's row.
     */
    private void update(String assignment, StatementBinder binder) {

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE players SET " + assignment + " WHERE bot_id = ? AND guild_id = ?")) {

            int next = binder.bind(statement);
            statement.setLong(next, BotIdentity.get());
            statement.setLong(next + 1, this.guildId);

            statement.executeUpdate();

        } catch (Exception ignored) { }

    }

    @FunctionalInterface
    private interface StatementBinder {
        /** Binds the assignment parameters and returns the next free index. */
        int bind(PreparedStatement statement) throws Exception;
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
     * Sets the current position (queue index) of the player.
     *
     * @param position The new position of the player.
     */
    public void setPosition(int position) {
        this.position = position;
        update("queue_position = ?", statement -> {
            statement.setInt(1, position);
            return 2;
        });
    }

    /**
     * Sets whether the player is playing and anchors the wall-clock playback
     * position at the same time (event-driven, never periodic).
     *
     * @param playing Whether the player is currently audible.
     * @param positionMs The playback position (ms) at this event.
     */
    public void setPlaying(boolean playing, long positionMs) {
        update("playing = ?, position_ms = ?, position_at = now()", statement -> {
            statement.setBoolean(1, playing);
            statement.setLong(2, positionMs);
            return 3;
        });
    }

    /**
     * Re-anchors the wall-clock playback position after a seek.
     *
     * @param positionMs The playback position (ms) just seeked to.
     */
    public void anchorPosition(long positionMs) {
        update("position_ms = ?, position_at = now()", statement -> {
            statement.setLong(1, positionMs);
            return 2;
        });
    }

    /**
     * Points the player row at its active queue session (NULL when idle).
     *
     * @param sessionId The active session id, or null to clear it.
     */
    public void setSessionId(Long sessionId) {
        update("session_id = ?", statement -> {
            if (sessionId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, sessionId);
            }
            return 2;
        });
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
        this.textChannelId = textChannelId;
        update("text_channel_id = ?", statement -> {
            statement.setLong(1, textChannelId);
            return 2;
        });
    }

    /**
     * Sets the voice channel id of the player.
     *
     * @param voiceChannelId The new voice channel id of the player.
     */
    public void setVoiceChannelId(long voiceChannelId) {
        update("voice_channel_id = ?", statement -> {
            statement.setLong(1, voiceChannelId);
            return 2;
        });
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
        this.autoplay = autoplayMode;
        update("autoplay = ?", statement -> {
            statement.setString(1, autoplayMode.name().toLowerCase());
            return 2;
        });
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
        this.loop = loopMode;
        update("loop_mode = ?", statement -> {
            statement.setString(1, loopMode.name().toLowerCase());
            return 2;
        });
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
        this.shuffle = shuffleMode;
        update("shuffle = ?", statement -> {
            statement.setString(1, shuffleMode.name().toLowerCase());
            return 2;
        });
    }

    /**
     * Gets the volume of the player.
     *
     * @return The volume of the player (0-200, default 100).
     */
    public int getVolume() {
        return this.volume;
    }

    /**
     * Sets the volume of the player.
     *
     * @param volume The new volume of the player.
     */
    public void setVolume(int volume) {
        this.volume = volume;
        update("volume = ?", statement -> {
            statement.setInt(1, volume);
            return 2;
        });
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
        this.speed = speed;
        update("speed = ?", statement -> {
            statement.setFloat(1, speed);
            return 2;
        });
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
        this.pitch = pitch;
        update("pitch = ?", statement -> {
            statement.setFloat(1, pitch);
            return 2;
        });
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
        this.bassBoost = bassBoost;
        update("bass_boost = ?", statement -> {
            statement.setFloat(1, bassBoost);
            return 2;
        });
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
        this.rotation = rotation;
        update("rotation = ?", statement -> {
            statement.setBoolean(1, rotation);
            return 2;
        });
    }

}
