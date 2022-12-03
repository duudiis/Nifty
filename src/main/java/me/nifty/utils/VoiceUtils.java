package me.nifty.utils;

import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.InactivityType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

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

        InactivityUtils.stopTimer(InactivityType.PAUSED, guild);
        InactivityUtils.stopTimer(InactivityType.STOPPED, guild);
        InactivityUtils.stopTimer(InactivityType.ALONE, guild);

        PlayerManager.destroy(guild);

    }

    /**
     * Disconnects the bot from the voice channel and clears up all the player
     * with an inactivity message being sent to the channel
     * @param guild The guild to disconnect from
     */
    public static void inactivityDisconnect(Guild guild) {

        PlayerManager playerManager = PlayerManager.get(guild);
        if (playerManager == null) { return; }

        TextChannel textChannel = guild.getTextChannelById(playerManager.getPlayerHandler().getTextChannelId());

        if (textChannel != null) {

            EmbedBuilder inactivityEmbed = new EmbedBuilder()
                    .setDescription("I left the voice channel because I was inactive for too long.\nIf you are a **Premium** member, you can disable this by typing `/247`.")
                    .setColor(guild.getSelfMember().getColor());

            textChannel.sendMessageEmbeds(inactivityEmbed.build()).queue(
                    message -> message.delete().queueAfter(2, java.util.concurrent.TimeUnit.MINUTES, null, ignored -> {}),
                    ignored -> {}
            );

        }

        AudioManager JDAAudioManager = guild.getAudioManager();
        JDAAudioManager.closeAudioConnection();

        PlayerManager.destroy(guild);

    }

}
