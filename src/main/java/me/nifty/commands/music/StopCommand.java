package me.nifty.commands.music;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class StopCommand extends BaseCommand {

    public StopCommand() {
        super("stop", List.of("st", "sotp"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        playerManager.getTrackScheduler().stop();

        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDED1")).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));
        playerManager.getTrackScheduler().stop();

        EmbedBuilder stopEmbed = new EmbedBuilder()
                .setDescription(":octagonal_sign: Stopped the music")
                .setColor(Objects.requireNonNull(event.getGuild()).getSelfMember().getColor());

        event.replyEmbeds(stopEmbed.build()).queue();

    }

    @Override
    public void executeAsVoice(String args, Guild guild, User user) {
        PlayerManager playerManager = PlayerManager.get(guild);
        playerManager.getTrackScheduler().stop();
    }

}
