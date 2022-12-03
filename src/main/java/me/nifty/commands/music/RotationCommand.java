package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.core.music.managers.AudioFiltersManager;
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

public class RotationCommand extends BaseCommand {

    public RotationCommand() {
        super("8d", List.of("rotation", "rotate", "rot"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = rotationCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = rotationCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    private Pair<Boolean, MessageEmbed> rotationCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        boolean currentRotationMode = audioFiltersManager.getRotation();
        boolean newRotationMode = BoolParser.parse(input, currentRotationMode);

        audioFiltersManager.setRotation(newRotationMode);

        EmbedBuilder rotationEmbed = new EmbedBuilder()
                .setDescription("8D mode has been " + (newRotationMode ? "**enabled**" : "**disabled**"))
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, rotationEmbed.build());

    }

}
