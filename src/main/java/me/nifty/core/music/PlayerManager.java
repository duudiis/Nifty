package me.nifty.core.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.handlers.AudioEventsHandler;
import me.nifty.core.music.handlers.AudioPlayerSendHandler;
import me.nifty.core.music.managers.AudioFiltersManager;
import me.nifty.core.music.managers.AutoplayManager;
import me.nifty.managers.AudioManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {

    private static final AudioPlayerManager audioManager = AudioManager.getAudioManager();
    private static final Map<Long, PlayerManager> guildPlayerManagers = new HashMap<>();

    private Guild guild;
    private AudioPlayer audioPlayer;
    private TrackScheduler trackScheduler;

    private AudioFiltersManager audioFiltersManager;
    private AutoplayManager autoplayManager;

    private PlayerHandler playerHandler;
    private QueueHandler queueHandler;

    /**
     * Creates a new Player Manager for the specified guild.
     * @param guild The guild to create the player manager for
     */
    public static void create(Guild guild) {

        if (guildPlayerManagers.containsKey(guild.getIdLong())) {
            throw new IllegalArgumentException("PlayerManager already exists for guild " + guild.getId());
        }

        // Creates the new player manager and adds it to the map
        PlayerManager playerManager = new PlayerManager();
        guildPlayerManagers.put(guild.getIdLong(), playerManager);

        // Sets the guild for the player manager
        playerManager.setGuild(guild);

        // Creates the audio player and sets it for the player manager
        playerManager.setPlayer(audioManager.createPlayer());

        // Creates the database player handler
        playerManager.setPlayerHandler(new PlayerHandler(guild.getIdLong()));

        // Creates the database queue handler
        playerManager.setQueueHandler(new QueueHandler(guild.getIdLong()));

        // Creates the track scheduler, add listener, and sets it for the player manager
        TrackScheduler trackScheduler = new TrackScheduler(playerManager);
        playerManager.setTrackScheduler(trackScheduler);

        // Creates and sets the event listener for the audio player
        AudioEventsHandler audioEventsHandler = new AudioEventsHandler(playerManager);
        playerManager.getAudioPlayer().addListener(audioEventsHandler);

        // Creates the audio filters manager and sets it for the player manager
        AudioFiltersManager audioFiltersManager = new AudioFiltersManager(playerManager);
        playerManager.setAudioFiltersManager(audioFiltersManager);

        // Creates the autoplay manager and sets it for the player manager
        AutoplayManager autoplayManager = new AutoplayManager(playerManager);
        playerManager.setAutoplayManager(autoplayManager);

        // Sets the sending handler for the guild's audio manager
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(playerManager.getAudioPlayer()));

    }

    /**
     * Gets the Player Manager for the specified guild.
     * @param guild The guild to get the player manager for
     * @return The Player Manager for the specified guild
     */
    public static PlayerManager get(Guild guild) {
        return guildPlayerManagers.get(guild.getIdLong());
    }

    /**
     * Destroys the Player Manager for the specified guild.
     * @param guild The guild to destroy the player manager
     */
    public static void destroy(Guild guild) {

        // Gets the player manager for the guild
        PlayerManager playerManager = get(guild);
        if (playerManager == null) { return; }

        // Clears the queue
        playerManager.getQueueHandler().clearQueue();

        // Deletes the player handler
        playerManager.getPlayerHandler().delete();

        // Gets the audio player from the player manager
        AudioPlayer audioPlayer = playerManager.getAudioPlayer();

        // Stops the audio player
        audioPlayer.setPaused(false);
        audioPlayer.stopTrack();

        // Destroys the audio player
        audioPlayer.destroy();

        // Removes the player manager from the map
        guildPlayerManagers.remove(guild.getIdLong());

    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public AudioPlayer getAudioPlayer() {
        return this.audioPlayer;
    }

    public void setPlayer(AudioPlayer player) {
        this.audioPlayer = player;
    }

    public TrackScheduler getTrackScheduler() {
        return this.trackScheduler;
    }

    public void setTrackScheduler(TrackScheduler trackScheduler) {
        this.trackScheduler = trackScheduler;
    }

    public PlayerHandler getPlayerHandler() {
        return this.playerHandler;
    }

    public void setPlayerHandler(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    public QueueHandler getQueueHandler() {
        return this.queueHandler;
    }

    public void setQueueHandler(QueueHandler queueHandler) {
        this.queueHandler = queueHandler;
    }

    public AudioFiltersManager getAudioFiltersManager() {
        return this.audioFiltersManager;
    }

    public void setAudioFiltersManager(AudioFiltersManager audioFiltersManager) {
        this.audioFiltersManager = audioFiltersManager;
    }

    public AutoplayManager getAutoplayManager() {
        return this.autoplayManager;
    }

    public void setAutoplayManager(AutoplayManager autoplayManager) {
        this.autoplayManager = autoplayManager;
    }

}
