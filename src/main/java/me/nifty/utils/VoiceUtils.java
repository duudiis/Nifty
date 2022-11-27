package me.nifty.utils;

import me.nifty.core.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Objects;

public class VoiceUtils {

    /**
     * Joins a voice channel
     * @param voiceChannel The voice channel to join
     * @return The error message if any, null if no error.
     */
    public static String join(VoiceChannel voiceChannel) {

        AudioManager JDAAudioManager = voiceChannel.getGuild().getAudioManager();

        try {

            // Sets to auto request to speak if the channel is a stage channel
            voiceChannel.getGuild().requestToSpeak();

            // Connects to the voice channel
            JDAAudioManager.openAudioConnection(voiceChannel);

            // Creates the player manager
            PlayerManager.create(voiceChannel.getGuild());

        } catch (Exception e) {

            // If an error occurs, rolls back the changes
            JDAAudioManager.closeAudioConnection();
            PlayerManager.destroy(voiceChannel.getGuild());

            if (e.getMessage() != null) {
                return e.getMessage();
            } else {
                return "An error occurred.";
            }

        }

        return null;

    }

    /**
     * Disconnects the bot from the voice channel and clears up all the player
     * @param guild The guild to disconnect from
     */
    public static void disconnect(Guild guild) {

        AudioManager JDAAudioManager = guild.getAudioManager();
        JDAAudioManager.closeAudioConnection();

        PlayerManager.destroy(guild);

    }

}
