package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Shuffle;
import me.nifty.websocket.WebSocketClientEndpoint;
import org.json.JSONObject;

/**
 * Serialises a guild's current player state and pushes it to the dashboard.
 * Pure add-on: when the dashboard is disconnected every call is a no-op.
 */
public class WsPlayer {

    public static void updateWsPlayer(PlayerManager playerManager) {

        // Don't bother building a payload when nothing is listening.
        if (!WebSocketClientEndpoint.isConnected()) { return; }

        if (playerManager == null) { return; }

        try {

            JSONObject base = new JSONObject();
            base.put("operation", "refresh_player");
            base.put("guildId", playerManager.getGuild().getId());

            AudioPlayer audioPlayer = playerManager.getAudioPlayer();
            AudioTrack playingTrack = audioPlayer.getPlayingTrack();

            // Nothing playing: tell the dashboard the player is empty for this guild.
            if (playingTrack == null) {
                base.put("data", new JSONObject());
                WebSocketClientEndpoint.send(base.toString());
                return;
            }

            JSONObject player = new JSONObject();
            player.put("progress", playingTrack.getPosition());
            player.put("playing", !audioPlayer.isPaused());
            player.put("shuffle", playerManager.getPlayerHandler().getShuffleMode() == Shuffle.ENABLED);
            player.put("loop", playerManager.getPlayerHandler().getLoopMode().name().toLowerCase());
            player.put("volume", audioPlayer.getVolume());
            player.put("position", playerManager.getPlayerHandler().getPosition());

            JSONObject track = new JSONObject();

            String formattedTitle = TrackTitle.format(playingTrack, 512);
            String title = formattedTitle;
            String artist = playingTrack.getInfo().author;

            if (formattedTitle.contains(" - ")) {
                title = formattedTitle.split("-", 2)[1].trim();
                artist = formattedTitle.split("-", 2)[0].trim();
            }

            track.put("title", title);
            track.put("artist", artist);
            track.put("artwork", Artwork.get(playingTrack)); // null -> dashboard falls back to placeholder
            track.put("songUrl", playingTrack.getInfo().uri);
            track.put("duration", playingTrack.getDuration());

            player.put("track", track);
            base.put("data", player);

            WebSocketClientEndpoint.send(base.toString());

        } catch (Exception ignored) {
            // Player serialisation must never break playback.
        }

    }

}
