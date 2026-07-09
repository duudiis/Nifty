package me.nifty.core.music.autoplay;

import com.github.topi314.lavasrc.deezer.DeezerAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.AudioManager;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Deezer "track mix" recommendations via LavaSrc ({@code dzrec:track=<id>}).
 *
 * <p>Seeds from any platform are matched into Deezer first by ISRC (which
 * Spotify, Apple Music, Tidal and Deezer tracks all carry) and otherwise by an
 * artist+title search — so a Tidal or Apple Music seed recommends just as well
 * as a native Deezer one. The returned tracks are native Deezer tracks and
 * play directly through the registered source manager.</p>
 */
public class DeezerRecommendations implements RecommendationProvider {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String API = "https://api.deezer.com/2.0";

    @Override
    public String name() {
        return "deezer";
    }

    @Override
    public boolean available() {
        try {
            return AudioManager.getAudioManager().source(DeezerAudioSourceManager.class) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public List<AudioTrack> recommend(ListeningContext.Seed seed, int limit) {

        Long deezerTrackId = resolveDeezerTrackId(seed);
        if (deezerTrackId == null) { return Collections.emptyList(); }

        List<AudioTrack> tracks = ProviderLoader.tracks("dzrec:track=" + deezerTrackId);

        return tracks.size() > limit ? tracks.subList(0, limit) : tracks;

    }

    /**
     * Maps a seed from any platform to its Deezer track id: native id, then
     * ISRC, then artist+title search — all via Deezer's public API.
     */
    private Long resolveDeezerTrackId(ListeningContext.Seed seed) {

        if ("deezer".equals(seed.source())) {
            try {
                return Long.parseLong(seed.sourceId());
            } catch (NumberFormatException ignored) { }
        }

        if (seed.isrc() != null && !seed.isrc().isBlank()) {
            JSONObject track = getJson(API + "/track/isrc:" + seed.isrc().trim());
            if (track != null && track.has("id")) {
                return track.getLong("id");
            }
        }

        String query = (seed.artist() + " " + seed.title()).trim();
        if (query.isBlank()) { return null; }

        JSONObject search = getJson(API + "/search?limit=1&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        if (search != null) {
            var data = search.optJSONArray("data");
            if (data != null && !data.isEmpty()) {
                return data.getJSONObject(0).optLong("id", 0) == 0 ? null : data.getJSONObject(0).getLong("id");
            }
        }

        return null;

    }

    /** GETs a Deezer public-API URL; null on any error or an API error body. */
    private JSONObject getJson(String url) {

        try {

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) { return null; }

            JSONObject json = new JSONObject(response.body());
            if (json.has("error")) { return null; }

            return json;

        } catch (Exception ignored) {
            return null;
        }

    }

}
