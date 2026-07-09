package me.nifty.core.music.autoplay;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * One source of "tracks similar to this one". Providers resolve the seed into
 * their own platform (directly, by ISRC, or by search) and return playable
 * {@link AudioTrack}s, ranked most-relevant first.
 *
 * <p>Implementations must be safe to call from the refill executor and fail
 * soft: no recommendations is an empty list, never an exception.</p>
 */
public interface RecommendationProvider {

    /** The provider's name, stored on buffer rows for provenance. */
    String name();

    /** Whether the provider can currently serve (its source manager is registered). */
    boolean available();

    /**
     * Recommends tracks similar to the seed.
     *
     * @param seed The seed track (a recently heard queue track).
     * @param limit A soft cap on how many tracks to return.
     * @return Ranked recommendations, empty when the provider has none.
     */
    List<AudioTrack> recommend(ListeningContext.Seed seed, int limit);

}
