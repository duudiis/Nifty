package me.nifty.websocket.payloads;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.Config;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.JDAManager;
import me.nifty.core.database.BotIdentity;
import me.nifty.utils.formatting.Artwork;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.websocket.DashboardSocket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Answers the dashboard's "which sessions can this user control?" question.
 *
 * <p>With several Nifty instances connected to one dashboard, each bot replies
 * with the guilds where the requesting user is in a voice channel. The dashboard
 * aggregates the replies from every bot into a single server/voice selector and
 * uses each entry's guild id to route control actions back to the right bot.</p>
 */
public class WsSessions {

    public static void reply(long userId) {

        if (!DashboardSocket.isConnected() || userId == 0) { return; }

        try {

            JDA jda = JDAManager.getJDA();
            if (jda == null) { return; }

            JSONArray sessions = new JSONArray();

            for (Guild guild : jda.getGuilds()) {

                Member member = guild.getMemberById(userId);
                if (member == null) { continue; }

                GuildVoiceState voiceState = member.getVoiceState();
                if (voiceState == null || !voiceState.inAudioChannel()) { continue; }

                AudioChannel userChannel = voiceState.getChannel();

                JSONObject session = new JSONObject();
                session.put("botId", String.valueOf(BotIdentity.get()));
                session.put("botName", Config.getDashboardBotName());
                session.put("guildId", guild.getId());
                session.put("guildName", guild.getName());
                session.put("guildIcon", guild.getIconUrl());
                session.put("voiceChannelId", userChannel != null ? userChannel.getId() : null);
                session.put("voiceChannelName", userChannel != null ? userChannel.getName() : null);

                // Is this bot connected and playing in this guild?
                PlayerManager playerManager = PlayerManager.get(guild.getIdLong());
                boolean botActive = playerManager != null;
                session.put("botActive", botActive);

                if (botActive) {
                    GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();
                    boolean sameChannel = selfVoiceState != null
                            && selfVoiceState.inAudioChannel()
                            && userChannel != null
                            && userChannel.equals(selfVoiceState.getChannel());
                    session.put("sameChannel", sameChannel);

                    AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();
                    if (playingTrack != null) {
                        JSONObject nowPlaying = new JSONObject();
                        nowPlaying.put("title", TrackTitle.format(playingTrack, 128));
                        nowPlaying.put("artwork", Artwork.get(playingTrack));
                        session.put("nowPlaying", nowPlaying);
                    }
                }

                sessions.put(session);

            }

            JSONObject data = new JSONObject();
            data.put("userId", String.valueOf(userId));
            data.put("sessions", sessions);

            JSONObject base = new JSONObject();
            base.put("operation", "sessions");
            base.put("botId", String.valueOf(BotIdentity.get()));
            base.put("data", data);

            DashboardSocket.send(base.toString());

        } catch (Exception ignored) {
            // Session discovery must never break the bot.
        }

    }

}
