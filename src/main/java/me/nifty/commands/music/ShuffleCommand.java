package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.enums.Shuffle;
import me.nifty.utils.formatting.WsPlayer;
import me.nifty.utils.parser.BoolParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class ShuffleCommand extends BaseCommand {

    public ShuffleCommand() {
        super("shuffle", List.of("shuffles", "shu", "sh", "random", "randomize", "randomizer"), "shuffle");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = shuffleCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = shuffleCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    private Pair<Boolean, MessageEmbed> shuffleCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        Shuffle currentShuffle = playerManager.getPlayerHandler().getShuffleMode();

        boolean newShuffleBool = BoolParser.parse(input, currentShuffle == Shuffle.ENABLED);
        Shuffle newShuffleMode = newShuffleBool ? Shuffle.ENABLED : Shuffle.DISABLED;

        playerManager.getPlayerHandler().setShuffleMode(newShuffleMode);

        if (newShuffleMode == Shuffle.ENABLED) {
            int currentPosition = playerManager.getPlayerHandler().getPosition();
            playerManager.getQueueHandler().shuffleAfter(currentPosition + 1);
        }

        EmbedBuilder shuffleEmbed = new EmbedBuilder()
                .setDescription("Shuffle mode has been **" + newShuffleMode.toString().toLowerCase() + "**")
                .setColor(guild.getSelfMember().getColor());

        WsPlayer.updateWsPlayer(playerManager);
        return new Pair<>(true, shuffleEmbed.build());

    }

}
