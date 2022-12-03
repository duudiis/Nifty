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

public class NightcoreCommand extends BaseCommand {

    public NightcoreCommand() {
        super("nightcore", List.of("nc", "night", "core"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = nightcoreCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = nightcoreCommand(event.getGuild(), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets the nightcore of the music player in the specified guild.
     *
     * @param guild The guild to set the speed in
     * @param input The nightcore to set
     * @return A pair containing a boolean representing whether the command was successful, and a message embed
     */
    private Pair<Boolean, MessageEmbed> nightcoreCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        boolean currentNightcore = audioFiltersManager.getSpeed() > 1.0f && audioFiltersManager.getPitch() > 1.0f;
        boolean newNightcore = BoolParser.parse(input, currentNightcore);

        if (newNightcore) {
            audioFiltersManager.setSpeed(1.3f);
            audioFiltersManager.setPitch(1.3f);
        } else {
            audioFiltersManager.setSpeed(1.0f);
            audioFiltersManager.setPitch(1.0f);
        }

        EmbedBuilder nightcoreEmbed = new EmbedBuilder()
                .setDescription("Nightcore mode has been **" + (newNightcore ? "enabled" : "disabled") + "**")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, nightcoreEmbed.build());

    }

}
