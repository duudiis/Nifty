package me.nifty.commands.music;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class ClearCommand extends BaseCommand {

    public ClearCommand() {
        super("clear", List.of("cl", "clr", "cls"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        playerManager.getTrackScheduler().clear();

        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4C")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));
        playerManager.getTrackScheduler().clear();

        EmbedBuilder clearEmbed = new EmbedBuilder()
                .setDescription("Cleared the queue")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(clearEmbed.build()).queue();

    }

}
