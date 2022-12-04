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

public class BassBoostCommand extends BaseCommand {

    public BassBoostCommand() {
        super("bassboost", List.of("bb", "bass", "bboost"), "music");

        this.requiresVoice = true;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = bassBoostCommand(event.getGuild(), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = event.getOption("value") == null ? "" : Objects.requireNonNull(event.getOption("value")).getAsString();

        Pair<Boolean, MessageEmbed> result = bassBoostCommand(event.getGuild(), query);

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Sets the bass boost of the music player in the specified guild.
     *
     * @param guild The guild to set the speed in
     * @param input The bass boost to set
     * @return A pair containing a boolean representing whether the command was successful, and a message embed
     */
    private Pair<Boolean, MessageEmbed> bassBoostCommand(Guild guild, String input) {

        PlayerManager playerManager = PlayerManager.get(guild);
        AudioFiltersManager audioFiltersManager = playerManager.getAudioFiltersManager();

        if (input == null || input.isEmpty()) {

            float currentBassBoost = audioFiltersManager.getBassBoost();

            EmbedBuilder bassBoostEmbed = new EmbedBuilder()
                    .setDescription("**" + currentBassBoost + "x**")
                    .setColor(guild.getSelfMember().getColor());

            return new Pair<>(true, bassBoostEmbed.build());

        }

        Float newBassBoost = FloatParser.parse(input);

        if (newBassBoost == null) {
            return new Pair<>(false, ErrorEmbed.get("Invalid float format! Try `0.9` or `1.3`"));
        }

        if (newBassBoost == -1f) { newBassBoost = 0.0f; }

        if (newBassBoost < 0.0f) { newBassBoost = 0.0f; }
        if (newBassBoost > 5.0f) { newBassBoost = 5.0f; }

        audioFiltersManager.setBassBoost(newBassBoost);

        EmbedBuilder bassBoostEmbed = new EmbedBuilder()
                .setDescription("BassBoost is now set to **" + newBassBoost + "x**")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, bassBoostEmbed.build());

    }

}
