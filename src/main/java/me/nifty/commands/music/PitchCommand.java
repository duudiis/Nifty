package me.nifty.commands.music;

import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.core.music.managers.AudioFiltersManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.parser.FloatParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class PitchCommand extends BaseCommand {

    public PitchCommand() {
        super("pitch", List.of("pt", "pit", "setpitch"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = pitchCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = event.getOption("value") == null ? "" : Objects.requireNonNull(event.getOption("value")).getAsString();

        Pair<Boolean, MessageEmbed> result = pitchCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets the pitch of the music player in the specified guild.
     *
     * @param guild The guild to set the speed in
     * @param input The pitch to set
     * @return A pair containing a boolean representing whether the command was successful, and a message embed
     */
    private Pair<Boolean, MessageEmbed> pitchCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        if (input == null || input.isEmpty()) {

            float currentPitch = audioFiltersManager.getPitch();

            EmbedBuilder pitchEmbed = new EmbedBuilder()
                    .setDescription("**" + currentPitch + "x**")
                    .setColor(guild.getSelfMember().getColor());

            return new Pair<>(true, pitchEmbed.build());

        }

        Float newPitch = FloatParser.parse(input);

        if (newPitch == null) {
            return new Pair<>(false, ErrorEmbed.get("Invalid float format! Try `0.9` or `1.3`"));
        }

        if (newPitch == -1f) { newPitch = 1f; }

        if (newPitch < 0.05f) { newPitch = 0.05f; }
        if (newPitch > 5.0f) { newPitch = 5.0f; }

        audioFiltersManager.setPitch(newPitch);

        EmbedBuilder pitchEmbed = new EmbedBuilder()
                .setDescription("Pitch is now set to **" + newPitch + "x**")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, pitchEmbed.build());

    }

}
