package me.nifty.core.music.autoplay;

import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.AudioManager;

import java.util.Collections;
import java.util.List;

/**
 * Spotify recommendations via LavaSrc ({@code sprec:mix:...}, the partner
 * "inspired by" mix with a v1-API fallback inside LavaSrc). Native Spotify
 * seeds use their track id; anything else with an ISRC still works via the
 * {@code mix:isrc} seed. Returned tracks mirror through a playable source at
 * playback time, like every Spotify track the bot plays.
 */
public class SpotifyRecommendations implements RecommendationProvider {

    @Override
    public String name() {
        return "spotify";
    }

    @Override
    public boolean available() {
        try {
            return AudioManager.getAudioManager().source(SpotifySourceManager.class) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public List<AudioTrack> recommend(ListeningContext.Seed seed, int limit) {

        String mixSeed;

        if ("spotify".equals(seed.source())) {
            mixSeed = "mix:track:" + seed.sourceId();
        } else if (seed.isrc() != null && !seed.isrc().isBlank()) {
            mixSeed = "mix:isrc:" + seed.isrc().trim();
        } else {
            return Collections.emptyList();
        }

        List<AudioTrack> tracks = ProviderLoader.tracks("sprec:" + mixSeed);

        return tracks.size() > limit ? tracks.subList(0, limit) : tracks;

    }

}
