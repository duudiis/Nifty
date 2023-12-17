package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.WsPlayer;
import me.nifty.utils.parser.LoopParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class LoopCommand extends BaseCommand {

    public LoopCommand() {
        super("loop", List.of("l", "looping", "repeat"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = loopCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = loopCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets a loop mode for a guild.
     *
     * @param guild The guild to set the loop mode in
     * @param input The query to parse the new loop mode from
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> loopCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        Autoplay currentAutoPlay = playerManager.getPlayerHandler().getAutoplayMode();

        if (currentAutoPlay != Autoplay.DISABLED) {
            return new Pair<>(false, ErrorEmbed.get("AutoPlay and Loop cannot both be enabled at the same time!"));
        }

        Loop newLoop = LoopParser.parse(input, playerManager);

        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (newLoop == Loop.TRACK) {
            embedBuilder.setDescription("Now looping the **current track**.");
        } else if (newLoop == Loop.QUEUE) {
            embedBuilder.setDescription("Now looping the **queue**.");
        } else {
            embedBuilder.setDescription("Looping is now **disabled**.");
        }

        embedBuilder.setColor(guild.getSelfMember().getColor());

        playerManager.getPlayerHandler().setLoopMode(newLoop);

        WsPlayer.updateWsPlayer(playerManager);
        return new Pair<>(true, embedBuilder.build());

    }

}
