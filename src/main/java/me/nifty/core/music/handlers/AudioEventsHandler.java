package me.nifty.core.music.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.formatting.NowPlayingEmbed;
import me.nifty.utils.formatting.TrackTitle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Timer;
import java.util.TimerTask;

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
    public void onTrackStart(AudioPlayer player, AudioTrack track) {

        TextChannel textChannel = playerManager.getGuild().getTextChannelById(playerHandler.getTextChannelId());
        if (textChannel == null) { return; }

        try {
            nowPlayingMessage = textChannel.sendMessageEmbeds(NowPlayingEmbed.get(track, playerManager)).complete();
        } catch (Exception e) {
            nowPlayingMessage = null;
        }

    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

        if (nowPlayingMessage != null) {
            try {
                nowPlayingMessage.delete().queue();
            } catch (Exception ignored) { }
        }

        if (endReason.mayStartNext) {

            Autoplay autoplayMode = playerHandler.getAutoplayMode();
            Loop loopMode = playerHandler.getLoopMode();

            if (loopMode == Loop.TRACK) {
                player.playTrack(track.makeClone());
                return;
            }

            int queueSize = queueHandler.getQueueSize();
            int position = playerHandler.getPosition();

            int newPosition = position + 1;

            if (loopMode == Loop.QUEUE) {

                if (queueSize > newPosition) {
                    audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
                    playerHandler.setPosition(newPosition);
                } else {
                    audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                    playerHandler.setPosition(0);
                }

            }

            if (autoplayMode == Autoplay.ENABLED) {
                playerManager.getAutoplayManager().autoplay();
                return;
            }

            if (loopMode == Loop.DISABLED) {

                if (queueSize > newPosition) {
                    audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
                    playerHandler.setPosition(newPosition);
                }

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

        try {
            textChannel.sendMessageEmbeds(trackErrorEmbed.build()).complete().delete().queueAfter(2, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception ignored) { }

    }

}
