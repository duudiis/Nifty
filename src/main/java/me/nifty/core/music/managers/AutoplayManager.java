package me.nifty.core.music.managers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.music.PlayerManager;
import me.nifty.core.music.autoplay.AutoplayStore;
import me.nifty.core.music.autoplay.DeezerRecommendations;
import me.nifty.core.music.autoplay.ListeningContext;
import me.nifty.core.music.autoplay.RecommendationProvider;
import me.nifty.core.music.autoplay.SpotifyRecommendations;
import me.nifty.core.music.autoplay.YoutubeRadioRecommendations;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.websocket.payloads.WsDelta;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Autoplay as a rolling recommendation buffer instead of a one-shot pick.
 *
 * <p>While enabled, the manager keeps {@code autoplay_tracks} stocked with
 * {@link #MIN_SIZE}–{@link #MAX_SIZE} upcoming recommendations — the dashboard
 * shows them as the "Next from: Autoplay" queue section, where users can
 * reorder, remove (negative feedback), or promote them into the real queue.
 * When the queue runs out, {@link #playNext()} feeds the head of the buffer
 * into the queue and plays it.</p>
 *
 * <p>Recommendations come from {@link RecommendationProvider}s (Deezer mixes
 * first, then YouTube radio, then Spotify), scored against the
 * {@link ListeningContext}: seed recency, who queued the seed and whether they
 * are in the voice channel, the listeners' broader taste, and the guild's
 * negative feedback (skipped autoplays, removed suggestions).</p>
 *
 * <p>Refills run on a shared background executor — never on an audio or
 * gateway thread — and are serialized per guild by {@code refillLock}.</p>
 */
public class AutoplayManager {

    /** Refill the buffer whenever it drops below this many tracks... */
    private static final int MIN_SIZE = 10;
    /** ...topping it back up to this many. */
    private static final int MAX_SIZE = 20;
    /** How many seeds a single refill may spend provider calls on. */
    private static final int MAX_SEEDS_PER_REFILL = 4;
    /** At most this many buffered/upcoming tracks per artist (variety). */
    private static final int ARTIST_CAP = 2;

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "nifty-autoplay");
                thread.setDaemon(true);
                return thread;
            });

    private final PlayerManager playerManager;
    private final AutoplayStore store;

    private final List<RecommendationProvider> providers = List.of(
            new DeezerRecommendations(),
            new YoutubeRadioRecommendations(),
            new SpotifyRecommendations()
    );

    private final AtomicBoolean refillScheduled = new AtomicBoolean(false);
    private final Object refillLock = new Object();

    public AutoplayManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.store = new AutoplayStore(playerManager.getGuild().getIdLong());
    }

    /* -------------------------------------------------------------------- */
    /*  Toggle                                                              */
    /* -------------------------------------------------------------------- */

    public boolean isEnabled() {
        return playerManager.getPlayerHandler().getAutoplayMode() == Autoplay.ENABLED;
    }

    /**
     * The single switch for autoplay, used by the command and the dashboard
     * alike. Enabling turns loop off (they are mutually exclusive) and starts
     * filling the buffer; disabling empties it.
     *
     * @param enabled The new autoplay state.
     */
    public void setEnabled(boolean enabled) {

        if (enabled == isEnabled()) { return; }

        if (enabled) {

            if (playerManager.getPlayerHandler().getLoopMode() != Loop.DISABLED) {
                playerManager.getPlayerHandler().setLoopMode(Loop.DISABLED);
            }

            playerManager.getPlayerHandler().setAutoplayMode(Autoplay.ENABLED);
            requestRefill();

        } else {

            playerManager.getPlayerHandler().setAutoplayMode(Autoplay.DISABLED);
            store.clear();

        }

        WsDelta.player(playerManager);
        WsDelta.autoplay(playerManager.getGuild().getIdLong());

    }

    /* -------------------------------------------------------------------- */
    /*  Playback: feed the queue from the buffer                            */
    /* -------------------------------------------------------------------- */

    /**
     * Consumes the head of the buffer into the queue and plays it — the
     * autoplay advance called when the queue has nothing left. Unplayable
     * rows are skipped; an empty buffer gets one synchronous refill attempt
     * (cold start) before giving up.
     *
     * @return true if a track started playing.
     */
    public boolean playNext() {

        if (consumeHead()) { return true; }

        // Cold start (just enabled / bot restarted): fill synchronously once.
        int added;
        synchronized (refillLock) {
            added = refill();
        }

        if (added > 0 && consumeHead()) { return true; }

        // An empty queue has nothing to seed from — that is not an error,
        // there was just never anything to continue.
        if (playerManager.getQueueHandler().getQueueSize() > 0) {
            autoplayError();
        }

        return false;

    }

    private boolean consumeHead() {

        // A few attempts: a stale cached blob that no longer decodes just
        // means that row is dropped and the next one plays.
        for (int attempt = 0; attempt < 5; attempt++) {

            AutoplayStore.Entry entry = store.takeFirst();
            if (entry == null) { return false; }

            AudioTrack track = entry.track();
            if (track == null) { continue; }

            track.setUserData(BotIdentity.get());

            int queueSize = playerManager.getQueueHandler().getQueueSize();

            playerManager.getQueueHandler().addTrack(track, queueSize);
            playerManager.getPlaybackAnalytics().trackQueued(BotIdentity.get(), track, "autoplay");

            // Set the cursor before playing: playTrack fires onTrackStart
            // synchronously, which pushes the player snapshot to the dashboard —
            // a cursor set afterwards would report the stale position.
            playerManager.getPlayerHandler().setPosition(queueSize);
            playerManager.getAudioPlayer().playTrack(track);

            WsDelta.autoplay(playerManager.getGuild().getIdLong());
            requestRefill();

            return true;

        }

        return false;

    }

    /* -------------------------------------------------------------------- */
    /*  Dashboard operations on the buffer                                  */
    /* -------------------------------------------------------------------- */

    /**
     * Removes a suggestion from the buffer and records it as negative
     * feedback, so the recommender stops proposing it.
     *
     * @param entryId The autoplay_tracks row id.
     * @param userId The removing user (0 = unknown).
     */
    public void removeEntry(long entryId, long userId) {

        AutoplayStore.Entry entry = store.take(entryId);
        if (entry == null) { return; }

        store.recordRemoval(entry.trackId(), userId);

        WsDelta.autoplay(playerManager.getGuild().getIdLong());
        requestRefill();

    }

    /**
     * Reorders a suggestion within the buffer (dashboard drag).
     *
     * @param entryId The autoplay_tracks row id.
     * @param toIndex The target index.
     */
    public void moveEntry(long entryId, int toIndex) {
        store.move(entryId, toIndex);
        WsDelta.autoplay(playerManager.getGuild().getIdLong());
    }

    /**
     * Moves a suggestion out of the buffer into the real queue, attributed to
     * the user who asked for it (it becomes their pick, not autoplay's).
     *
     * @param entryId The autoplay_tracks row id.
     * @param userId The promoting user's id.
     * @param placement "now" (play immediately), "next", or "end".
     */
    public void promoteEntry(long entryId, long userId, String placement) {

        AutoplayStore.Entry entry = store.take(entryId);
        if (entry == null || entry.track() == null) {
            WsDelta.autoplay(playerManager.getGuild().getIdLong());
            return;
        }

        AudioTrack track = entry.track();
        track.setUserData(userId != 0 ? userId : BotIdentity.get());

        int queueSize = playerManager.getQueueHandler().getQueueSize();
        int position = "end".equals(placement) || queueSize == 0
                ? queueSize
                : playerManager.getPlayerHandler().getPosition() + 1;

        playerManager.getQueueHandler().addTrack(track, position);
        playerManager.getPlaybackAnalytics().trackQueued(track.getUserData() instanceof Long id ? id : 0L, track, "dashboard");

        WsDelta.autoplay(playerManager.getGuild().getIdLong());

        if ("now".equals(placement)) {
            playerManager.getTrackScheduler().jump(position);
        }

        requestRefill();

    }

    /**
     * The queue grew (a user added tracks): drop buffered suggestions the
     * queue now contains and top the buffer back up against the new context.
     */
    public void onQueueExpanded() {

        if (!isEnabled()) { return; }

        EXECUTOR.execute(() -> {
            try {
                if (store.removeQueueDuplicates() > 0) {
                    WsDelta.autoplay(playerManager.getGuild().getIdLong());
                }
            } catch (Exception ignored) { }
            requestRefill();
        });

    }

    /**
     * Empties the buffer without touching the autoplay setting (player
     * destroyed — mirrors the queue being cleared).
     */
    public void clearBuffer() {
        store.clear();
        WsDelta.autoplay(playerManager.getGuild().getIdLong());
    }

    /* -------------------------------------------------------------------- */
    /*  Refill                                                              */
    /* -------------------------------------------------------------------- */

    /**
     * Schedules a background refill (debounced: bursts of queue activity
     * collapse into one run). No-op while autoplay is off.
     */
    public void requestRefill() {

        if (!isEnabled()) { return; }
        if (!refillScheduled.compareAndSet(false, true)) { return; }

        EXECUTOR.schedule(() -> {
            refillScheduled.set(false);
            try {
                synchronized (refillLock) {
                    refill();
                }
            } catch (Exception ignored) {
                // Refills must never take anything down with them.
            }
        }, 750, TimeUnit.MILLISECONDS);

    }

    /** Runs one refill if the buffer is low. Callers hold {@code refillLock}. */
    private int refill() {

        if (!isEnabled()) { return 0; }

        int size = store.size();
        if (size >= MIN_SIZE) { return 0; }

        ListeningContext context = ListeningContext.build(playerManager);
        if (context.seeds.isEmpty()) { return 0; }

        List<Candidate> picked = pickCandidates(context, MAX_SIZE - size);

        int added = 0;
        for (Candidate candidate : picked) {
            if (store.append(candidate.track, candidate.provider, candidate.seedTrackId)) {
                added++;
            }
        }

        if (added > 0) {
            WsDelta.autoplay(playerManager.getGuild().getIdLong());
        }

        return added;

    }

    /* -------------------------------------------------------------------- */
    /*  Candidate gathering & scoring                                       */
    /* -------------------------------------------------------------------- */

    private static class Candidate {
        final AudioTrack track;
        final String provider;
        final long seedTrackId;
        final String artistKey;
        double score;

        Candidate(AudioTrack track, String provider, long seedTrackId, String artistKey, double score) {
            this.track = track;
            this.provider = provider;
            this.seedTrackId = seedTrackId;
            this.artistKey = artistKey;
            this.score = score;
        }
    }

    /**
     * Queries providers seed by seed (strongest seeds first, first provider
     * that answers wins for that seed), scores every fresh candidate against
     * the context, and greedily picks the best ones under a per-artist cap.
     */
    private List<Candidate> pickCandidates(ListeningContext context, int wanted) {

        List<Candidate> pool = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int seedsUsed = 0;

        for (ListeningContext.Seed seed : context.seeds) {

            if (seedsUsed >= MAX_SEEDS_PER_REFILL || pool.size() >= wanted * 3) { break; }

            for (RecommendationProvider provider : providers) {

                if (!provider.available()) { continue; }

                List<AudioTrack> recommendations;
                try {
                    recommendations = provider.recommend(seed, 25);
                } catch (Exception ignored) {
                    continue;
                }

                if (recommendations.isEmpty()) { continue; }

                int rank = 0;
                for (AudioTrack track : recommendations) {
                    Candidate candidate = toCandidate(track, provider.name(), seed, rank++, context);
                    if (candidate != null && seen.add(candidateKey(track))) {
                        pool.add(candidate);
                    }
                }

                seedsUsed++;
                break; // this seed is served — spend the next call on another seed

            }

        }

        pool.sort((a, b) -> Double.compare(b.score, a.score));

        // Greedy pick under the artist cap, counting what's already queued up.
        Map<String, Integer> artistCounts = new HashMap<>(context.artistCounts);
        List<Candidate> picked = new ArrayList<>();

        for (Candidate candidate : pool) {
            if (picked.size() >= wanted) { break; }

            int count = artistCounts.getOrDefault(candidate.artistKey, 0);
            if (count >= ARTIST_CAP) { continue; }

            artistCounts.put(candidate.artistKey, count + 1);
            picked.add(candidate);
        }

        return picked;

    }

    /** Scores one raw recommendation; null when it is excluded or unusable. */
    private Candidate toCandidate(AudioTrack track, String provider, ListeningContext.Seed seed,
                                  int rank, ListeningContext context) {

        if (track == null || track.getInfo().isStream) { return null; }

        String artist = track.getInfo().author;
        String title = track.getInfo().title;

        // Excluded: already in the queue/buffer, or recently rejected here —
        // matched across platforms by id, ISRC and normalized artist|title.
        if (context.excludedSourceKeys.contains(candidateKey(track))) { return null; }
        String isrc = track.getInfo().isrc;
        if (isrc != null && context.excludedIsrcs.contains(isrc.toUpperCase(Locale.ROOT))) { return null; }
        String titleKey = ListeningContext.titleKey(artist, title);
        if (context.excludedTitleKeys.contains(titleKey)) { return null; }

        String artistKey = artist == null ? "" : artist.toLowerCase(Locale.ROOT);

        // Score: how strong the seed is × how high the provider ranked it,
        // boosted for artists the room's listeners actually queue, dampened
        // for artists whose autoplays keep getting skipped here.
        double score = seed.weight() / (1.0 + rank * 0.15);
        score *= context.artistBoost.getOrDefault(artistKey, 1.0);
        score /= (1.0 + 0.5 * context.artistSkips.getOrDefault(artistKey, 0));

        return new Candidate(track, provider, seed.trackId(), artistKey, score);

    }

    private static String candidateKey(AudioTrack track) {
        String source = track.getSourceManager() != null ? track.getSourceManager().getSourceName() : "unknown";
        return source + ":" + track.getIdentifier();
    }

    /* -------------------------------------------------------------------- */
    /*  Errors                                                              */
    /* -------------------------------------------------------------------- */

    public void autoplayError() {

        TextChannel textChannel = playerManager.getGuild()
                .getTextChannelById(playerManager.getPlayerHandler().getTextChannelId());
        if (textChannel == null) { return; }

        try {
            textChannel.sendMessageEmbeds(ErrorEmbed.get("Could not find anything to AutoPlay!")).queue();
        } catch (Exception ignored) { }

    }

}
