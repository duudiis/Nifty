package me.nifty.utils.formatting;

import com.github.topisenpai.lavasrc.deezer.DeezerAudioTrack;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Shuffle;
import me.nifty.websocket.WebSocketClientEndpoint;
import org.json.JSONObject;

public class WsPlayer {

    public static void updateWsPlayer(PlayerManager playerManager) {

        JSONObject base = new JSONObject();
        base.put("operation", "refresh_player");

        if (playerManager == null) {
            WebSocketClientEndpoint.send(base.put("data", new JSONObject()).toString());
            return;
        }

        base.put("guildId", playerManager.getGuild().getId());

        AudioPlayer audioPlayer = playerManager.getAudioPlayer();

        AudioTrack playingTrack = audioPlayer.getPlayingTrack();

        if (playingTrack == null) {
            WebSocketClientEndpoint.send(base.put("data", new JSONObject()).toString());
            return;
        }

        JSONObject player = new JSONObject();

        player.put("progress", playingTrack.getPosition());
        player.put("playing", !audioPlayer.isPaused());
        player.put("shuffle", playerManager.getPlayerHandler().getShuffleMode() == Shuffle.ENABLED);
        player.put("loop", playerManager.getPlayerHandler().getLoopMode().name().toLowerCase());
        player.put("volume", audioPlayer.getVolume());

        JSONObject track = new JSONObject();

        String formattedTitle = TrackTitle.format(playingTrack, 512);

        String title = formattedTitle;
        String artist = playingTrack.getInfo().author;

        if (formattedTitle.contains(" - ")) {
            title = formattedTitle.split("-")[1].trim();
            artist = formattedTitle.split("-")[0].trim();
        }

        String artwork = "https://i.ytimg.com/vi/" + playingTrack.getInfo().identifier + "/mqdefault.jpg";

        if (playingTrack instanceof MirroringAudioTrack) {
            artwork = ((MirroringAudioTrack) playingTrack).getArtworkURL();
        }

        if (playingTrack instanceof DeezerAudioTrack) {
            artwork = ((DeezerAudioTrack) playingTrack).getArtworkURL();
        }

        track.put("title", title);
        track.put("artist", artist);
        track.put("artwork", artwork);
        track.put("songUrl", playingTrack.getInfo().uri);
        track.put("artistUrl", "https://www.youtube.com/channel/");
        track.put("duration", playingTrack.getDuration());

        player.put("track", track);

        base.put("data", player);

        WebSocketClientEndpoint.send(base.toString());

    }

}
