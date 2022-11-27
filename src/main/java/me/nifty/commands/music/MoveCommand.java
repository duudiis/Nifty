package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.utils.parser.RangeParser;
import me.nifty.utils.parser.TrackParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class MoveCommand extends BaseCommand {

    public MoveCommand() {
        super("move", List.of("mv", "mov"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        Pair<Boolean, MessageEmbed> result = moveCommand(event.getGuild(), args);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        String position = Objects.requireNonNull(event.getOption("position")).getAsString();

        String input = track + " " + position;

        Pair<Boolean, MessageEmbed> result = moveCommand(event.getGuild(), input.split(" +"));

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Moves a track to a new position in the queue.
     *
     * @param guild The guild to move the track in.
     * @param args The arguments to move the track with.
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> moveCommand(Guild guild, String[] args) {

        PlayerManager playerManager = PlayerManager.get(guild);

        if (args.length == 0) {
            return new Pair<>(false, ErrorEmbed.get("You must specify a track to move!"));
        }

        if (args.length == 1) {
            return new Pair<>(false, ErrorEmbed.get("You must specify a position to move to!"));
        }

        String[] trackArgs = new String[args.length - 1];
        System.arraycopy(args, 0, trackArgs, 0, args.length - 1);

        String positionQuery = args[args.length - 1];
        int newPosition = TrackParser.parse(positionQuery, playerManager);

        if (newPosition == -1) {
            return new Pair<>(false, ErrorEmbed.get("The new position \"" + positionQuery + "\" is not valid!"));
        }

        String trackQuery = String.join(" ", trackArgs);
        int trackPosition = TrackParser.parse(trackQuery, playerManager);

        AudioTrack movedTrack = playerManager.getQueueHandler().getQueueTrack(trackPosition);

        if (trackPosition != -1 && movedTrack != null) {

            int movePosition = adjustPosition(playerManager, trackPosition, newPosition);

            EmbedBuilder movedEmbed = new EmbedBuilder()
                    .setDescription("Moved [" + TrackTitle.format(movedTrack, 64) + "]" +
                            "(" + movedTrack.getInfo().uri + ") " +
                            "to position **" + (movePosition + 1) + "**")
                    .setColor(guild.getSelfMember().getColor());

            playerManager.getTrackScheduler().move(trackPosition, movePosition);
            return new Pair<>(true, movedEmbed.build());
        }

        int[] moveRange = RangeParser.parse(trackQuery);

        if (moveRange[0] != -1) {

            int queueSize = playerManager.getQueueHandler().getQueueSize();

            int startPosition = Math.max(moveRange[0], 0);
            int endPosition = Math.min(moveRange[1], queueSize - 1);

            int amount = (endPosition - startPosition) + 1;

            int movePosition = adjustPosition(playerManager, startPosition, newPosition);

            EmbedBuilder movedManyEmbed = new EmbedBuilder()
                    .setDescription("Moved **" + amount + "** track" + (amount == 1 ? "" : "s") + "" +
                            " to position **" + (movePosition + 1) + "**")
                    .setColor(guild.getSelfMember().getColor());

            playerManager.getTrackScheduler().moveRange(startPosition, endPosition, movePosition);
            return new Pair<>(true, movedManyEmbed.build());

        }

        return new Pair<>(false, ErrorEmbed.get("A track could not be found for \"" + trackQuery + "\"!"));

    }

    /**
     * Adjusts the position of the track to be moved to account for
     * relative positions from the current position, for a better user experience.
     *
     * @param playerManager The player manager for the guild.
     * @param trackPosition The position of the track to be moved.
     * @param newPosition The position to move the track to.
     * @return The adjusted position.
     */
    private int adjustPosition(PlayerManager playerManager, int trackPosition, int newPosition) {

        int currentPosition = playerManager.getPlayerHandler().getPosition();

        if (currentPosition == 0) {
            return newPosition;
        } else if (currentPosition == newPosition && newPosition < trackPosition) {
            return newPosition + 1;
        } else if ((currentPosition + 1) == newPosition && newPosition > trackPosition) {
            return newPosition - 1;
        } else if ((currentPosition - 1) == newPosition && newPosition < trackPosition) {
            return newPosition + 1;
        }

        return newPosition;

    }

}
