package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Resolves the best available cover artwork URL for a track across the
 * different audio sources the bot supports. Used by the dashboard add-on.
 *
 * <p>This never throws: if no artwork can be resolved it returns {@code null}
 * and the dashboard is expected to fall back to a placeholder image.</p>
 */
public class Artwork {

    /**
     * Resolves an artwork URL for the given track.
     *
     * @param track The track to resolve artwork for.
     * @return A best-effort artwork URL, or {@code null} if none is available.
     */
    public static String get(AudioTrack track) {

        if (track == null) {
            return null;
        }

        try {

            // LavaSrc sources (Spotify, Apple Music, Deezer, Tidal, …) populate
            // the standard LavaPlayer artwork field, so this one check covers them all.
            String infoArtwork = track.getInfo().artworkUrl;
            if (isUsable(infoArtwork)) {
                return infoArtwork;
            }

            // YouTube / YouTube Music: derive the thumbnail from the video identifier.
            String identifier = track.getInfo().identifier;
            String source = track.getSourceManager() != null ? track.getSourceManager().getSourceName() : "";

            if (isUsable(identifier) && ("youtube".equals(source) || "youtubemusic".equals(source))) {
                // Full-resolution, bar-free cover.
                return "https://i.ytimg.com/vi/" + identifier + "/maxresdefault.jpg";
            }

        } catch (Exception ignored) {
            // Never let artwork resolution break a player update.
        }

        return null;

    }

    private static boolean isUsable(String url) {
        return url != null && !url.isBlank();
    }

}
