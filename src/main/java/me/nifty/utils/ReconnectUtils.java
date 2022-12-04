package me.nifty.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReconnectUtils {

    public static void reconnectPlayers(JDA jda) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM Players");
            ResultSet result = selectStatement.executeQuery();

            while (result.next()) {

                String guildId = result.getString("guild_id");
                String voiceId = result.getString("voice_id");

                Guild guild = jda.getGuildById(guildId);
                if (guild == null) { continue; }

                VoiceChannel voiceChannel = guild.getVoiceChannelById(voiceId);
                if (voiceChannel == null) { continue; }

                VoiceUtils.join(voiceChannel);

                PlayerManager playerManager = PlayerManager.get(guild);
                if (playerManager == null) { continue; }

                playerManager.getAudioFiltersManager().updateFilterFactory();

                boolean wasPlaying = result.getBoolean("playing");

                if (wasPlaying) {

                    AudioPlayer audioPlayer = playerManager.getAudioPlayer();
                    int position = result.getInt("position");

                    audioPlayer.playTrack(playerManager.getQueueHandler().getQueueTrack(position));

                }

            }

        } catch (Exception ignored) { }

    }

}
