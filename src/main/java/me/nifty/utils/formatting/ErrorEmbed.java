package me.nifty.utils.formatting;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.Objects;

public class ErrorEmbed {

    private static final Color errorColor = Color.decode("#ff0000");

    /**
     * Formats an error message to a message embed.
     *
     * @param message The error message to format.
     * @return The formatted message embed.
     */
    public static MessageEmbed get(String message) {

            EmbedBuilder errorEmbed = new EmbedBuilder();

            errorEmbed.setDescription(Objects.requireNonNullElse(message, "An error occurred."));
            errorEmbed.setColor(errorColor);

            return errorEmbed.build();

    }

}
