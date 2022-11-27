package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.utils.parser.RangeParser;
import me.nifty.utils.parser.TrackParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class RemoveCommand extends BaseCommand {

    public RemoveCommand() {
        super("remove", List.of("r", "rm", "delete", "del"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        if (args.length == 0) {
            return;
        }

        String query = String.join(" ", args);

        MessageEmbed removeEmbed = removeCommand(event.getGuild(), query);

        event.getChannel().sendMessageEmbeds(removeEmbed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = Objects.requireNonNull(event.getOption("input")).getAsString();

        MessageEmbed removeReply = removeCommand(event.getGuild(), query);

        event.replyEmbeds(removeReply).queue();

    }

    // TODO: Add a way to send the embed before the tracks are removed.

    /**
     * Removes a track or multiple tracks from the queue.
     *
     * @param guild The guild to remove the track from.
     * @param query The query to remove the track with.
     * @return The message embed to return.
     */
    private MessageEmbed removeCommand(Guild guild, String query) {

        PlayerManager playerManager = PlayerManager.get(guild);

        int trackPosition = TrackParser.parse(query, playerManager);
        AudioTrack removedTrack = playerManager.getQueueHandler().getQueueTrack(trackPosition);

        if (trackPosition != -1 && removedTrack != null) {

            EmbedBuilder removedEmbed = new EmbedBuilder()
                    .setDescription("Removed [" + TrackTitle.format(removedTrack, 64) + "]" +
                            "(" + removedTrack.getInfo().uri + ") " +
                            "[<@!" + removedTrack.getUserData() + ">]")
                    .setColor(guild.getSelfMember().getColor());

            playerManager.getTrackScheduler().remove(trackPosition);
            return removedEmbed.build();

        }

        int[] trackRange = RangeParser.parse(query);

        if (trackRange[0] != -1) {

            int queueSize = playerManager.getQueueHandler().getQueueSize();

            int startPosition = Math.max(trackRange[0], 0);
            int endPosition = Math.min(trackRange[1], queueSize - 1);

            int amount = (endPosition - startPosition) + 1;

            EmbedBuilder removedManyEmbed = new EmbedBuilder()
                    .setDescription("Removed **" + amount + "** track" + (amount == 1 ? "" : "s"))
                    .setColor(guild.getSelfMember().getColor());

            playerManager.getTrackScheduler().removeRange(startPosition, endPosition);
            return removedManyEmbed.build();

        }

        return ErrorEmbed.get("A track could not be found for \"" + query + "\"");

    }

}
