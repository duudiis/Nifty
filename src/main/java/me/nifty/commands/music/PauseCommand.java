package me.nifty.commands.music;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class PauseCommand extends BaseCommand {

    public PauseCommand() {
        super("pause", List.of("pa", "time"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        playerManager.getTrackScheduler().pause();

        event.getMessage().addReaction(Emoji.fromUnicode("\u23F8")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));
        playerManager.getTrackScheduler().pause();

        EmbedBuilder pauseEmbed = new EmbedBuilder()
                .setDescription(":pause_button: Paused the player")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(pauseEmbed.build()).queue();

    }

}
