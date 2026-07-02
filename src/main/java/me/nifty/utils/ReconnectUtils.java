package me.nifty.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReconnectUtils {

    private record PlayerRow(long guildId, long voiceChannelId, boolean playing, int position) { }

    /**
     * Restores this bot instance's players after a restart: rejoins the saved
     * voice channels and resumes the saved queue positions.
     *
     * @param jda The ready JDA instance
     */
    public static void reconnectPlayers(JDA jda) {

        // Snapshot the rows first — reconnecting mutates the players table.
        List<PlayerRow> rows = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT guild_id, voice_channel_id, playing, queue_position FROM players WHERE bot_id = ?")) {

            statement.setLong(1, BotIdentity.get());

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                rows.add(new PlayerRow(
                        result.getLong("guild_id"),
                        result.getLong("voice_channel_id"),
                        result.getBoolean("playing"),
                        result.getInt("queue_position")
                ));
            }

        } catch (Exception ignored) { }

        for (PlayerRow row : rows) {

            try {

                Guild guild = jda.getGuildById(row.guildId());

                if (guild == null) {
                    new PlayerHandler(row.guildId()).delete();
                    new QueueHandler(row.guildId()).clearQueue();
                    continue;
                }

                VoiceChannel voiceChannel = guild.getVoiceChannelById(row.voiceChannelId());
                if (voiceChannel == null) { VoiceUtils.disconnect(guild); continue; }

                String joinResult = VoiceUtils.join(voiceChannel);
                if (joinResult != null) { VoiceUtils.disconnect(guild); continue; }

                PlayerManager playerManager = PlayerManager.get(guild);
                if (playerManager == null) { VoiceUtils.disconnect(guild); continue; }

                playerManager.getAudioFiltersManager().updateFilterFactory();

                if (row.playing()) {
                    AudioPlayer audioPlayer = playerManager.getAudioPlayer();
                    audioPlayer.playTrack(playerManager.getQueueHandler().getQueueTrack(row.position()));
                }

            } catch (Exception ignored) { }

        }

    }

}
