package me.nifty.core.music.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.nifty.core.database.guild.GuildHandler;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.InactivityUtils;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.InactivityType;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;
import me.nifty.utils.formatting.NowPlayingEmbed;
import me.nifty.utils.formatting.TrackTitle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class AudioEventsHandler extends AudioEventAdapter {

    private final PlayerManager playerManager;

    private final AudioPlayer audioPlayer;

    private final PlayerHandler playerHandler;
    private final QueueHandler queueHandler;

    private Message nowPlayingMessage;

    public AudioEventsHandler(PlayerManager playerManager) {
        this.playerManager = playerManager;

        this.audioPlayer = playerManager.getAudioPlayer();

        this.playerHandler = playerManager.getPlayerHandler();
        this.queueHandler = playerManager.getQueueHandler();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {

        // Sets the player to playing on the database
        playerHandler.setPlaying(true);

        // Cancels the inactivity timer
        InactivityUtils.stopTimer(InactivityType.PAUSED, playerManager.getGuild());

        // If there is a now playing message
        if (nowPlayingMessage != null) {
            // Edit the message to show that the player is playing
            nowPlayingMessage.editMessageEmbeds(NowPlayingEmbed.get(audioPlayer.getPlayingTrack(), playerManager)).queue(null, ignored -> {});
        }

    }

    @Override
    public void onPlayerPause(AudioPlayer player) {

        // Sets the player to not playing on the database
        playerHandler.setPlaying(false);

        // Creates the inactivity timer
        InactivityUtils.startTimer(InactivityType.PAUSED, playerManager.getGuild());

        // If there is a now playing message
        if (nowPlayingMessage != null) {
            // Edit the message to show that the player is paused
            nowPlayingMessage.editMessageEmbeds(NowPlayingEmbed.get(audioPlayer.getPlayingTrack(), playerManager)).queue(null, ignored -> {});
        }

    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {

        // Sets the player to playing on the database
        playerHandler.setPlaying(true);

        // Cancels the inactivity timer
        InactivityUtils.stopTimer(InactivityType.STOPPED, playerManager.getGuild());

        // Gets the announces text channel for the player
        TextChannel textChannel = playerManager.getGuild().getTextChannelById(playerHandler.getTextChannelId());

        // If there is a text channel
        if (textChannel != null) {

            boolean announcementsMode = GuildHandler.getAnnouncementsMode(playerManager.getGuild().getIdLong());

            if (announcementsMode) {
                // Send a now playing message
                textChannel.sendMessageEmbeds(NowPlayingEmbed.get(track, playerManager)).queue(
                        message -> nowPlayingMessage = message,
                        error -> nowPlayingMessage = null
                );
            }

        }

    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

        // Sets the player to not playing on the database
        playerHandler.setPlaying(false);

        // Creates the inactivity timer
        InactivityUtils.startTimer(InactivityType.STOPPED, playerManager.getGuild());

        // If there is a now playing message
        if (nowPlayingMessage != null) {
            // Deletes the now playing message
            nowPlayingMessage.delete().queue(null, ignored -> {});
        }

        // If the end reason is not because the track was stopped
        if (endReason.mayStartNext) {

            // Gets the loop mode for the player
            Loop loopMode = playerHandler.getLoopMode();
            Autoplay autoplayMode = playerHandler.getAutoplayMode();

            // If the loop mode is set to loop the track
            if (loopMode == Loop.TRACK) {
                // Plays the same track again
                player.playTrack(track.makeClone());
                return;
            }

            // Gets the queue size and current position
            int queueSize = queueHandler.getQueueSize();
            int position = playerHandler.getPosition();

            // Sets the new position to the current position + 1
            int newPosition = position + 1;

            // If the new position is greater than the queue size
            if (queueSize > newPosition) {
                // Sets the new position to the current position and plays the track
                audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
                playerHandler.setPosition(newPosition);
            } else if (loopMode == Loop.QUEUE) {
                // If the loop mode is set to loop the queue

                // Gets the shuffle mode for the player
                Shuffle shuffleMode = playerHandler.getShuffleMode();

                // If the shuffle mode is set to shuffle the queue
                if (shuffleMode == Shuffle.ENABLED) {
                    // Shuffles the entire queue
                    queueHandler.shuffleAfter(0);
                }

                // Sets the new position to 0 and plays the track
                audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                playerHandler.setPosition(0);

            } else if (autoplayMode == Autoplay.ENABLED) {
                // If the autoplay mode is set to autoplay

                // Gets the next track from the autoplay and plays it
                playerManager.getAutoplayManager().autoplay();

            }

        }

    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {

        TextChannel textChannel = playerManager.getGuild().getTextChannelById(playerHandler.getTextChannelId());
        if (textChannel == null) { return; }

        EmbedBuilder trackErrorEmbed = new EmbedBuilder()
                .setTitle("An error occurred while playing")
                .setDescription("[" + TrackTitle.format(track, 63) + "](" + track.getInfo().uri + ") " +
                        "[<@!" + track.getUserData() + ">]\n" + exception.getMessage());


        textChannel.sendMessageEmbeds(trackErrorEmbed.build()).queue(
                message -> message.delete().queueAfter(2, java.util.concurrent.TimeUnit.MINUTES, null, ignored -> {}),
                ignored -> {}
        );

    }

}
