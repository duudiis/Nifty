package me.nifty.commands.music;

import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class JoinCommand extends BaseCommand {

    public JoinCommand() {
        super("join", List.of("connect", "summon"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4C")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder joinedEmbed = new EmbedBuilder()
                .setDescription("Joined your voice channel :blush:")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(joinedEmbed.build()).queue();

    }

}
