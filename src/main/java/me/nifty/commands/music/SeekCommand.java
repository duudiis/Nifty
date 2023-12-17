package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTime;
import me.nifty.utils.formatting.WsPlayer;
import me.nifty.utils.parser.TimeParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class SeekCommand extends BaseCommand {

    public SeekCommand() {
        super("seek", List.of("seekto"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());

        if (playerManager == null) {
            event.getChannel().sendMessageEmbeds(ErrorEmbed.get("You must be playing a track to use this command!")).queue();
            return;
        }

        if (playerManager.getAudioPlayer().getPlayingTrack() == null) {

            int currentPosition = playerManager.getPlayerHandler().getPosition();

            AudioTrack lastTrack = playerManager.getQueueHandler().getQueueTrack(currentPosition);

            if (lastTrack == null) {
                event.getChannel().sendMessageEmbeds(ErrorEmbed.get("You must be playing a track to use this command!")).queue();
                return;
            }

            playerManager.getTrackScheduler().jump(currentPosition);

        }

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = seekCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4C")).queue();
        } else {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = seekCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Seeks to a specific time in the current track in the specified guild.
     *
     * @param guild The guild to seek in
     * @param input The time to seek to
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> seekCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        if (playerManager == null || playerManager.getAudioPlayer().getPlayingTrack() == null) {
            return new Pair<>(false, ErrorEmbed.get("You must be playing a track to use this command!"));
        }

        if (input == null || input.isEmpty()) {
            return new Pair<>(false, ErrorEmbed.get("You must specify a time to seek to!"));
        }

        long seekTime = TimeParser.parse(input);;

        if (seekTime == -1) {
            return new Pair<>(false, ErrorEmbed.get("Invalid time format! Try `1:30` or `1m 30s`"));
        }

        playerManager.getAudioPlayer().getPlayingTrack().setPosition(seekTime);

        EmbedBuilder seekEmbed = new EmbedBuilder()
                .setDescription("Seeked to " + TrackTime.formatNatural(seekTime))
                .setColor(guild.getSelfMember().getColor());

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                WsPlayer.updateWsPlayer(playerManager);
            }
        }, 500);

        return new Pair<>(true, seekEmbed.build());

    }

}
