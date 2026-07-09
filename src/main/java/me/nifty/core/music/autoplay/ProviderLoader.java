package me.nifty.core.music.autoplay;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.AudioManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Blocking convenience wrapper around {@code AudioPlayerManager.loadItem} for
 * the recommendation providers: one identifier in, a plain track list out,
 * bounded by a timeout so a hung lookup can never stall a refill for long.
 */
final class ProviderLoader {

    private static final long TIMEOUT_SECONDS = 10;

    private ProviderLoader() { }

    /**
     * Loads an identifier and returns its tracks: a playlist's tracks, a
     * single track as a one-element list, and no match / failure / timeout as
     * an empty list.
     */
    static List<AudioTrack> tracks(String identifier) {

        CompletableFuture<List<AudioTrack>> future = new CompletableFuture<>();

        try {

            AudioManager.getAudioManager().loadItem(identifier, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    future.complete(Collections.singletonList(track));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    future.complete(playlist.getTracks());
                }

                @Override
                public void noMatches() {
                    future.complete(Collections.emptyList());
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    future.complete(Collections.emptyList());
                }
            });

            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (Exception ignored) {
            return Collections.emptyList();
        }

    }

    /** Loads an identifier and returns just its first track, or null. */
    static AudioTrack first(String identifier) {
        List<AudioTrack> tracks = tracks(identifier);
        return tracks.isEmpty() ? null : tracks.get(0);
    }

}
