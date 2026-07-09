package me.nifty.core.music.autoplay;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * YouTube Music radio recommendations (the {@code RD<videoId>} mix playlist) —
 * the universal fallback: it needs no credentials and any seed can be matched
 * into YouTube by search, so autoplay always has at least this source.
 */
public class YoutubeRadioRecommendations implements RecommendationProvider {

    @Override
    public String name() {
        return "youtube";
    }

    @Override
    public boolean available() {
        return true; // the YouTube source manager is always registered
    }

    @Override
    public List<AudioTrack> recommend(ListeningContext.Seed seed, int limit) {

        String videoId = resolveVideoId(seed);
        if (videoId == null) { return List.of(); }

        List<AudioTrack> mix = ProviderLoader.tracks(
                "https://music.youtube.com/watch?v=" + videoId + "&list=RD" + videoId);

        // The mix usually opens with the seed itself — drop it.
        List<AudioTrack> tracks = new ArrayList<>();
        for (AudioTrack track : mix) {
            if (videoId.equals(track.getIdentifier())) { continue; }
            tracks.add(track);
            if (tracks.size() >= limit) { break; }
        }

        return tracks;

    }

    /** Native id for YouTube seeds, otherwise the top search hit. */
    private String resolveVideoId(ListeningContext.Seed seed) {

        if ("youtube".equals(seed.source())) {
            return seed.sourceId();
        }

        String query = (seed.artist() + " " + seed.title()).trim();
        if (query.isBlank()) { return null; }

        AudioTrack match = ProviderLoader.first("ytsearch:" + query);
        return match != null ? match.getIdentifier() : null;

    }

}
