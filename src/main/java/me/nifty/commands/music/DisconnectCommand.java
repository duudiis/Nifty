package me.nifty.commands.music;

import me.nifty.structures.BaseCommand;
import me.nifty.utils.VoiceUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class DisconnectCommand extends BaseCommand {

    public DisconnectCommand() {
        super("disconnect", List.of("dc", "leave", "fuckoff", "die", "quit"), "music");

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        VoiceUtils.disconnect(event.getGuild());
        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4B")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        VoiceUtils.disconnect(Objects.requireNonNull(event.getGuild()));

        EmbedBuilder disconnectedEmbed = new EmbedBuilder()
                .setDescription("Reset the player")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(disconnectedEmbed.build()).queue();

    }

}
