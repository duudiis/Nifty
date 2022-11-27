package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.utils.parser.TrackParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class JumpCommand extends BaseCommand {

    public JumpCommand() {
        super("jump", List.of("j"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = jumpCommand(event.getGuild(), query);

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

        String query = event.getOption("input") == null ? "" : Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = jumpCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Jumps to a specific track in the queue in the specified guild.
     *
     * @param guild The guild to jump in
     * @param input The track query to jump to
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> jumpCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        int trackPosition = TrackParser.parse(input, playerManager);

        if (trackPosition == -1) {
            return new Pair<>(false, ErrorEmbed.get("A track could not be found for \"" + input + "\""));
        }

        AudioTrack jumpTrack = playerManager.getQueueHandler().getQueueTrack(trackPosition);

        EmbedBuilder jumpEmbed = new EmbedBuilder()
                .setDescription("Jumped to [" + TrackTitle.format(jumpTrack, 64) + "](" + jumpTrack.getInfo().uri + ") [<@!" + jumpTrack.getUserData() + ">]")
                .setColor(guild.getSelfMember().getColor());

        playerManager.getTrackScheduler().jump(trackPosition);

        return new Pair<>(true, jumpEmbed.build());

    }

}
