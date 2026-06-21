package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.websocket.WebSocketClientEndpoint;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Pushes a guild's full queue to the dashboard.
 *
 * <p>Unlike the legacy implementation (which only pinged the dashboard to make
 * it re-query a shared MySQL database) this sends the entire track list inline.
 * The dashboard therefore needs no database of its own — the bot remains the
 * single source of truth.</p>
 */
public class WsQueue {

    public static void updateWsQueue(long guildId) {

        if (!WebSocketClientEndpoint.isConnected()) { return; }

        try {

            PlayerManager playerManager = PlayerManager.get(guildId);
            if (playerManager == null) { return; }

            Guild guild = playerManager.getGuild();

            JSONArray tracks = new JSONArray();

            List<AudioTrack> queueTracks = playerManager.getQueueHandler().getAllTracks();

            // List index == track position (track_id), kept contiguous by QueueHandler.
            for (int trackId = 0; trackId < queueTracks.size(); trackId++) {

                AudioTrack audioTrack = queueTracks.get(trackId);

                JSONObject track = new JSONObject();
                track.put("track_id", trackId);

                String formattedTitle = TrackTitle.format(audioTrack, 512);
                String title = formattedTitle;
                String artist = audioTrack.getInfo().author;

                if (formattedTitle.contains(" - ")) {
                    title = formattedTitle.split("-", 2)[1].trim();
                    artist = formattedTitle.split("-", 2)[0].trim();
                }

                track.put("title", title);
                track.put("artist", artist);
                track.put("artwork", Artwork.get(audioTrack));
                track.put("duration", audioTrack.getDuration());
                track.put("songUrl", audioTrack.getInfo().uri);

                // Resolve a friendly "added by" name from cache; fall back to the id.
                long memberId = audioTrack.getUserData() instanceof Long ? (Long) audioTrack.getUserData() : 0L;
                track.put("added_by_id", String.valueOf(memberId));

                String addedBy = String.valueOf(memberId);
                String addedByAvatar = null;
                if (guild != null && memberId != 0) {
                    Member member = guild.getMemberById(memberId);
                    if (member != null) {
                        addedBy = member.getEffectiveName();
                        addedByAvatar = member.getEffectiveAvatarUrl() + "?size=128";
                    }
                }
                track.put("added_by", addedBy);
                if (addedByAvatar != null) {
                    track.put("added_by_avatar", addedByAvatar);
                }

                tracks.put(track);

            }

            JSONObject data = new JSONObject();
            data.put("guildId", String.valueOf(guildId));
            data.put("position", playerManager.getPlayerHandler().getPosition());
            data.put("tracks", tracks);

            JSONObject base = new JSONObject();
            base.put("operation", "refresh_queue");
            base.put("guildId", String.valueOf(guildId));
            base.put("data", data);

            WebSocketClientEndpoint.send(base.toString());

        } catch (Exception ignored) {
            // Queue serialisation must never break playback.
        }

    }

}
