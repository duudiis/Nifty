package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.core.music.managers.AudioFiltersManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.parser.BoolParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class VaporwaveCommand extends BaseCommand {

    public VaporwaveCommand() {
        super("vaporwave", List.of("vw", "vapor", "vap", "slowed", "wave"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = vaporwaveCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = vaporwaveCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets the vaporwave of the music player in the specified guild.
     *
     * @param guild The guild to set the speed in
     * @param input The vaporwave to set
     * @return A pair containing a boolean representing whether the command was successful, and a message embed
     */
    private Pair<Boolean, MessageEmbed> vaporwaveCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        boolean currentVaporwave = audioFiltersManager.getSpeed() < 1.0f && audioFiltersManager.getPitch() < 1.0f;
        boolean newVaporwave = BoolParser.parse(input, currentVaporwave);

        if (newVaporwave) {
            audioFiltersManager.setSpeed(0.9f);
            audioFiltersManager.setPitch(0.85f);
        } else {
            audioFiltersManager.setSpeed(1.0f);
            audioFiltersManager.setPitch(1.0f);
        }

        EmbedBuilder vaporwaveEmbed = new EmbedBuilder()
                .setDescription("Vaporwave mode has been **" + (newVaporwave ? "enabled" : "disabled") + "**")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, vaporwaveEmbed.build());

    }

}
