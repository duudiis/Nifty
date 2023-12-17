package me.nifty.structures;

import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.List;

public class BaseCommand {

    private final String name;
    private final List<String> aliases;
    private final String category;

    protected boolean requiresVoice = false;

    protected boolean deferReply = false;
    protected boolean ephemeral = true;

    /**
     * Creates a new BaseCommand.
     *
     * @param name The name of the command.
     * @param aliases The aliases of the command.
     * @param category The category of the command.
     */
    public BaseCommand(String name, List<String> aliases, String category) {
        this.name = name;
        this.aliases = aliases;
        this.category = category;
    }

    /**
     * Executes the command in the message context.
     *
     * @param event The event.
     * @param args The arguments.
     */
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        MessageEmbed errorEmbed = ErrorEmbed.get("This command is not available in the message context.");
        event.getChannel().sendMessageEmbeds(errorEmbed).queue();

    }

    /**
     * Executes the command in the slash context.
     *
     * @param event The event.
     */
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        MessageEmbed errorEmbed = ErrorEmbed.get("This command is not available in the slash context.");

        if (deferReply) {
            event.getHook().sendMessageEmbeds(errorEmbed).queue();
        } else {
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue();
        }

    }

    public void executeAsVoice(String commandArgs, Guild guild, User user) {



    }

    /**
     * Gets the name of the command.
     *
     * @return The name of the command.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the aliases of the command.
     *
     * @return The aliases of the command.
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Gets the category of the command.
     *
     * @return The category of the command.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets whether the command requires the user to be in a voice channel.
     *
     * @return Whether the command requires the user to be in a voice channel.
     */
    public boolean requiresVoice() {
        return requiresVoice;
    }

    /**
     * Gets whether the command should defer the reply in the slash context.
     *
     * @return Whether the command should defer the reply.
     */
    public boolean requiresDeferReply() {
        return deferReply;
    }

    /**
     * Gets whether the command should be ephemeral in the slash context.
     *
     * @return Whether the command should be ephemeral.
     */
    public boolean isEphemeral() {
        return ephemeral;
    }

}