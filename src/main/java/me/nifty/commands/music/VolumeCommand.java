package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.core.music.managers.AudioFiltersManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.WsPlayer;
import me.nifty.utils.parser.FloatParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class VolumeCommand extends BaseCommand {

    public VolumeCommand() {
        super("volume", List.of("vol", "v"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = volumeCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = volumeCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    private Pair<Boolean, MessageEmbed> volumeCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioPlayer audioPlayer = playerManager.getAudioPlayer();

        if (input == null || input.isEmpty()) {

            int currentVolume = audioPlayer.getVolume();

            EmbedBuilder volumeEmbed = new EmbedBuilder()
                    .setDescription("**" + currentVolume + "%**")
                    .setColor(guild.getSelfMember().getColor());

            return new Pair<>(true, volumeEmbed.build());

        }

        Float newVolume = FloatParser.parse(input);

        if (newVolume == null) {
            return new Pair<>(false, ErrorEmbed.get("Invalid volume format! Try `100` or `200`"));
        }

        if (newVolume == -1f) { newVolume = 100f; }

        if (newVolume < 0f) { newVolume = 0f; }
        if (newVolume > 400f) { newVolume = 400f; }

        audioPlayer.setVolume(newVolume.intValue());

        EmbedBuilder volumeEmbed = new EmbedBuilder()
                .setDescription("Volume is now set to **" + newVolume + "%**")
                .setColor(guild.getSelfMember().getColor());

        WsPlayer.updateWsPlayer(playerManager);
        return new Pair<>(true, volumeEmbed.build());

    }

}
