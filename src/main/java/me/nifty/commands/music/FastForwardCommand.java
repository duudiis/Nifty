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

public class FastForwardCommand extends BaseCommand {

    public FastForwardCommand() {
        super("fastforward", List.of("ff", "fwd", "forward"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = fastForwardCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.getMessage().addReaction(Emoji.fromUnicode("\u23E9")).queue();
        } else {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = event.getOption("input") == null ? "" : Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = fastForwardCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Fast forwards the current track.
     *
     * @param guild The guild to fast-forward the track in.
     * @param input The input to fast-forward the track with.
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> fastForwardCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        if (playerManager == null || playerManager.getAudioPlayer().getPlayingTrack() == null) {
            return new Pair<>(false, ErrorEmbed.get("You must be playing a track to use this command!"));
        }

        long fastForwardTime = input.equals("") ? 15000 : TimeParser.parse(input);;

        if (fastForwardTime == -1) {
            return new Pair<>(false, ErrorEmbed.get("Invalid time format! Try `1:30` or `1m 30s`"));
        }

        long newPosition = playerManager.getAudioPlayer().getPlayingTrack().getPosition() + fastForwardTime;
        playerManager.getAudioPlayer().getPlayingTrack().setPosition(newPosition);

        EmbedBuilder fastForwardedEmbed = new EmbedBuilder()
                .setDescription("Fast forwarded " + TrackTime.formatNatural(fastForwardTime))
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, fastForwardedEmbed.build());

    }

}
