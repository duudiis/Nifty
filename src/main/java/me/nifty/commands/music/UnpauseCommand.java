package me.nifty.commands.music;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class UnpauseCommand extends BaseCommand {

    public UnpauseCommand() {
        super("unpause", List.of("unpa", "resume", "up", "continue", "replay"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        playerManager.getTrackScheduler().unpause();

        event.getMessage().addReaction(Emoji.fromUnicode("\u25B6\uFE0F")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));
        playerManager.getTrackScheduler().unpause();

        EmbedBuilder unpauseEmbed = new EmbedBuilder()
                .setDescription(":arrow_forward: Unpaused the player")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(unpauseEmbed.build()).queue();

    }

}
