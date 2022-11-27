package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class NowPlayingEmbed {

    /**
     * Formats the currently playing track to a message embed.
     *
     * @param playerManager The player manager to use to get the currently playing track.
     * @return The formatted message embed.
     */
    public static MessageEmbed get(AudioTrack track, PlayerManager playerManager) {

        EmbedBuilder nowPlayingEmbed = new EmbedBuilder()
                .setTitle("Now Playing")
                .setDescription("[" + TrackTitle.format(track, 64) + "](" + track.getInfo().uri + ") [<@!" + track.getUserData() + ">]")
                .setColor(playerManager.getGuild().getSelfMember().getColor());

        // If the track is paused, adds the paused message to the footer
        if (playerManager.getAudioPlayer().isPaused()) {
            nowPlayingEmbed.setFooter("The bot is currently paused.");
        }

        return nowPlayingEmbed.build();

    }

}
