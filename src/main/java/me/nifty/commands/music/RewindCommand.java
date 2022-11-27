package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTime;
import me.nifty.utils.parser.TimeParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class RewindCommand extends BaseCommand {

    public RewindCommand() {
        super("rewind", List.of("rew", "rwd", "rw"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = rewindCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.getMessage().addReaction(Emoji.fromUnicode("\u23EA")).queue();
        } else {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = event.getOption("input") == null ? "" : Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = rewindCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Rewinds the current track in the specified guild by the specified amount of time.
     *
     * @param guild The guild to rewind the track in.
     * @param input The amount of time to rewind the track by.
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> rewindCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        if (playerManager == null || playerManager.getAudioPlayer().getPlayingTrack() == null) {
            return new Pair<>(false, ErrorEmbed.get("You must be playing a track to use this command!"));
        }

        long rewindTime = input.equals("") ? 15000 : TimeParser.parse(input);

        if (rewindTime == -1) {
            return new Pair<>(false, ErrorEmbed.get("Invalid time format! Try `1:30` or `1m 30s`"));
        }

        long newPosition = playerManager.getAudioPlayer().getPlayingTrack().getPosition() - rewindTime;
        playerManager.getAudioPlayer().getPlayingTrack().setPosition(newPosition);

        EmbedBuilder rewindEmbed = new EmbedBuilder()
                .setDescription("Rewound " + TrackTime.formatNatural(rewindTime))
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, rewindEmbed.build());

    }

}