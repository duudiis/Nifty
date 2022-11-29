package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.parser.BoolParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class AutoplayCommand extends BaseCommand {

    public AutoplayCommand() {
        super("autoplay", List.of("ap", "auto"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = autoplayCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = autoplayCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }


    }

    private Pair<Boolean, MessageEmbed> autoplayCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);

        Loop currentLoopMode = playerManager.getPlayerHandler().getLoopMode();

        if (currentLoopMode != Loop.DISABLED) {
            return new Pair<>(false, ErrorEmbed.get("AutoPlay and Loop cannot both be enabled at the same time!"));
        }

        Autoplay currentAutoPlay = playerManager.getPlayerHandler().getAutoplayMode();

        boolean newAutoplayMode = BoolParser.parse(input, currentAutoPlay == Autoplay.ENABLED);

        playerManager.getPlayerHandler().setAutoplayMode(newAutoplayMode ? Autoplay.ENABLED : Autoplay.DISABLED);

        EmbedBuilder autoplayEmbed = new EmbedBuilder()
                .setDescription("AutoPlay is now " + (newAutoplayMode ? "**enabled**" : "**disabled**"))
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, autoplayEmbed.build());

    }

}
