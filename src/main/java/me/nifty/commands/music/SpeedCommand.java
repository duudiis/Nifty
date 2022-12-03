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
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class SpeedCommand extends BaseCommand {

    public SpeedCommand() {
        super("speed", List.of("sp", "sped", "setspeed"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = speedCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = event.getOption("input") == null ? "" : Objects.requireNonNull(event.getOption("input")).getAsString();

        Pair<Boolean, MessageEmbed> result = speedCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets the speed of the music player in the specified guild.
     *
     * @param guild The guild to set the speed in
     * @param input The speed to set
     * @return A pair containing a boolean representing whether the command was successful, and a message embed
     */
    private Pair<Boolean, MessageEmbed> speedCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        if (input == null || input.isEmpty()) {

            float currentSpeed = audioFiltersManager.getSpeed();

            EmbedBuilder speedEmbed = new EmbedBuilder()
                    .setDescription("**" + currentSpeed + "x**")
                    .setColor(guild.getSelfMember().getColor());

            return new Pair<>(true, speedEmbed.build());

        }

        Float newSpeed = FloatParser.parse(input);

        if (newSpeed == null) {
            return new Pair<>(false, ErrorEmbed.get("Invalid float format! Try `0.9` or `1.3`"));
        }

        if (newSpeed < 0.05f) { newSpeed = 0.05f; }
        if (newSpeed > 5.0f) { newSpeed = 5.0f; }

        audioFiltersManager.setSpeed(newSpeed);

        EmbedBuilder speedEmbed = new EmbedBuilder()
                .setDescription("Speed is now set to **" + newSpeed + "x**")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, speedEmbed.build());

    }

}
