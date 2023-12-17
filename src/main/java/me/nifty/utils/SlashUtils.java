package me.nifty.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class SlashUtils {

    public static void registerSlashCommands(JDA jda) {

        // Registers the slash commands
        jda.updateCommands()
                .addCommands(Commands.slash("8d", "Toggles 8D mode"))
                .addCommands(Commands.slash("247", "Toggles 24/7 mode"))
                .addCommands(Commands.slash("announce", "Toggles whether Nifty will announce when songs start playing"))
                .addCommands(Commands.slash("autoplay", "Toggles whether Nifty will automatically play related songs after the queue has run out"))
                .addCommands(Commands.slash("back", "Goes back a song"))
                .addCommands(Commands.slash("bassboost", "Change or display the bass boost")
                        .addOption(OptionType.INTEGER, "value", "The value to set the bass boost to", false))
                .addCommands(Commands.slash("clear", "Clears the queue"))
                .addCommands(Commands.slash("disconnect", "Resets the player, clears the queue, and leaves the voice channel"))
                .addCommands(Commands.slash("fastforward", "Fast forwards the specified amount in the current song")
                        .addOption(OptionType.STRING, "input", "The amount to fast forward", false))
                .addCommands(Commands.slash("help", "Displays basic info about Nifty"))
                .addCommands(Commands.slash("invite", "Add the bot to another server"))
                .addCommands(Commands.slash("join", "Makes the bot join your voice channel"))
                .addCommands(Commands.slash("jump", "Jumps to the specified song in the queue")
                        .addOption(OptionType.STRING, "input", "The position or title of the track you want to jump to", true))
                .addCommands(
                        Commands.slash("loop", "Changes the looping mode")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "mode", "The mode to set the loop to", true)
                                                .addChoice("track", "track")
                                                .addChoice("queue", "queue")
                                                .addChoice("disabled", "disabled")
                                ))
                .addCommands(Commands.slash("move", "Move a song to a new position")
                        .addOption(OptionType.STRING, "track", "The track to move", true)
                        .addOption(OptionType.INTEGER, "position", "The new position to move the track to", true))
                .addCommands(Commands.slash("nightcore", "Toggles nightcore mode"))
                .addCommands(Commands.slash("now", "Display the playing track")
                        .addSubcommands(new SubcommandData("playing", "Display the playing track")))
                .addCommands(Commands.slash("pause", "Pauses the player"))
                .addCommands(Commands.slash("pitch", "Change or display the pitch")
                        .addOption(OptionType.INTEGER, "value", "The value to set the pitch to", false))
                .addCommands(
                        Commands.slash("play", "Play a song in your voice channel")
                                .addOption(OptionType.STRING, "input", "A search term or a link", true, true))
                .addCommands(Commands.slash("queue", "Displays the current queue of tracks"))
                .addCommands(Commands.slash("remove", "Removes the specified song")
                        .addOption(OptionType.STRING, "input", "The position or title of the track you want to remove", true))
                .addCommands(Commands.slash("rewind", "Rewinds the specified amount in the current song")
                        .addOption(OptionType.STRING, "input", "The amount to rewind", false))
                .addCommands(Commands.slash("search", "Searches for the input and returns a list of results for you to pick from")
                        .addOption(OptionType.STRING, "input", "The artist name or track title to search for", true, true))
                .addCommands(Commands.slash("seek", "Seeks the playing track to the specified timestamp")
                        .addOption(OptionType.STRING, "input", "The timestamp to seek to", true))
                .addCommands(Commands.slash("shuffle", "Shuffles the queue"))
                .addCommands(Commands.slash("skip", "Skips to the next song"))
                .addCommands(Commands.slash("speed", "Change or display the speed")
                        .addOption(OptionType.INTEGER, "value", "The value to set the speed to", false))
                .addCommands(Commands.slash("stop", "Stops the music"))
                .addCommands(Commands.slash("unpause", "Unpauses the player"))
                .addCommands(Commands.slash("vaporwave", "Toggles vaporwave mode"))
                .queue();

    }

}