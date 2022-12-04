package me.nifty.core.music.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Shuffle;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.SearchResultSelectMenu;
import me.nifty.utils.formatting.TrackTitle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import java.util.Collections;
import java.util.List;

public class AudioResultHandler implements AudioLoadResultHandler {

    private final AudioPlayer audioPlayer;

    private final PlayerHandler playerHandler;
    private final QueueHandler queueHandler;

    private final TextChannel textChannel;
    private final InteractionHook interactionHook;

    private final Member member;

    private final List<String> flags;

    public AudioResultHandler(PlayerManager playerManager, Object event, Member member, List<String> flags) {
        this.audioPlayer = playerManager.getAudioPlayer();

        this.playerHandler = playerManager.getPlayerHandler();
        this.queueHandler = playerManager.getQueueHandler();

        if (event instanceof TextChannel) {
            this.textChannel = (TextChannel) event;
            this.interactionHook = null;
        } else {
            this.textChannel = null;
            this.interactionHook = (InteractionHook) event;
        }

        this.member = member;
        this.flags = flags;
    }

    public void replyEvent(MessageEmbed embed) {

        if (textChannel != null) {
            textChannel.sendMessageEmbeds(embed).queue();
        } else if (interactionHook != null) {
            interactionHook.sendMessageEmbeds(embed).queue();
        }

    }

    public void replyEvent(MessageEmbed embed, SelectMenu selectMenu) {

        if (textChannel != null) {
            textChannel.sendMessageEmbeds(embed).setActionRow(selectMenu).queue();
        } else if (interactionHook != null) {
            interactionHook.sendMessageEmbeds(embed).setActionRow(selectMenu).queue();
        }

    }

    @Override
    public void trackLoaded(AudioTrack track) {

        track.setUserData(member.getUser().getIdLong());

        EmbedBuilder trackEmbed = new EmbedBuilder()
                .setDescription("Queued [" + TrackTitle.format(track, 64) + "](" + track.getInfo().uri + ") [" + member.getAsMention() + "]")
                .setColor(member.getGuild().getSelfMember().getColor());

        if (audioPlayer.isPaused()) {
            trackEmbed.setFooter("The bot is currently paused.");
        }

        replyEvent(trackEmbed.build());

        int queueSize = queueHandler.getQueueSize();
        int queuePosition = flags.contains("next") && queueSize > 0 ? playerHandler.getPosition() + 1 : queueSize;

        queueHandler.addTrack(track, queuePosition);

        AudioTrack playingTrack = audioPlayer.getPlayingTrack();

        if (playingTrack == null || flags.contains("jump")) {
            audioPlayer.playTrack(track);
            playerHandler.setPosition(queuePosition);
        }

    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {

        AudioTrack selectedTrack = playlist.getSelectedTrack();

        if (selectedTrack != null && !flags.contains("all")) {
            trackLoaded(selectedTrack);
            return;
        }

        if (playlist.isSearchResult()) {

            if (flags.contains("choose")) {

                EmbedBuilder searchEmbed = new EmbedBuilder()
                        .setDescription("Select the tracks you want to add to the queue.")
                        .setColor(member.getGuild().getSelfMember().getColor());

                SelectMenu searchMenu = SearchResultSelectMenu.get(playlist.getTracks(), member.getIdLong(), flags);

                replyEvent(searchEmbed.build(), searchMenu);

            } else {
                trackLoaded(playlist.getTracks().get(0));
            }

            return;

        }

        List<AudioTrack> audioTracks = playlist.getTracks();

        for (AudioTrack track : audioTracks) {
            track.setUserData(member.getUser().getIdLong());
        }

        Shuffle shuffleMode = playerHandler.getShuffleMode();

        if (shuffleMode == Shuffle.ENABLED || flags.contains("shuffle")) {
            Collections.shuffle(audioTracks);
        }

        EmbedBuilder playlistEmbed = new EmbedBuilder()
                .setDescription("Queued **" + audioTracks.size() + "** tracks [" + member.getAsMention() + "]")
                .setColor(member.getGuild().getSelfMember().getColor());

        if (audioPlayer.isPaused()) {
            playlistEmbed.setFooter("The bot is currently paused.");
        }

        replyEvent(playlistEmbed.build());

        int queueSize = queueHandler.getQueueSize();
        int queuePosition = flags.contains("next") && queueSize > 0 ? playerHandler.getPosition() + 1 : queueSize;

        queueHandler.addTracks(audioTracks, queuePosition);

        AudioTrack playingTrack = audioPlayer.getPlayingTrack();

        if (playingTrack == null || flags.contains("jump")) {
            audioPlayer.playTrack(audioTracks.get(0));
            playerHandler.setPosition(queuePosition);
        }

    }

    @Override
    public void noMatches() {
        replyEvent(ErrorEmbed.get("No matches found."));
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        replyEvent(ErrorEmbed.get(exception.getMessage()));
    }
}
