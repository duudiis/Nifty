package me.nifty.core.music.autoplay;

import me.nifty.core.analytics.PlaybackAnalytics;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.UserStore.UserRef;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A point-in-time snapshot of everything the recommender needs to know about
 * one guild's listening session, read from the analytics tables + a JDA
 * voice-channel snapshot:
 *
 * <ul>
 *   <li><b>Seeds</b> — the recently played (and next few upcoming) queue
 *       tracks, weighted by recency, by whether the user who queued them is
 *       in the voice channel right now, and down-weighted when the track was
 *       itself autoplayed.</li>
 *   <li><b>Exclusions</b> — everything in the queue and buffer, plus tracks
 *       the guild recently rejected: autoplayed plays that got skipped
 *       (track_plays: queued_by IS NULL, end_reason 'skipped') and buffer
 *       removals from the dashboard (autoplay_feedback). Matched by source id,
 *       ISRC and normalized "artist|title" so the same song can't sneak back
 *       in from a different platform.</li>
 *   <li><b>Artist signals</b> — a boost for artists the current listeners
 *       queue often (their taste, not just this queue's), and a penalty for
 *       artists whose autoplayed tracks keep getting skipped here.</li>
 * </ul>
 */
public class ListeningContext {

    /** How far back past the cursor seeds are taken from. */
    private static final int SEED_LOOKBACK = 12;
    /** How many upcoming (user-queued) tracks also count as seeds. */
    private static final int SEED_LOOKAHEAD = 4;
    /** How long a skip/removal keeps a track out of recommendations. */
    private static final String FEEDBACK_WINDOW = "30 days";
    /** How far back listener taste (artist boosts) is read from. */
    private static final String TASTE_WINDOW = "90 days";

    /** One weighted recommendation seed (a recently heard queue track). */
    public record Seed(long trackId, String source, String sourceId, String isrc,
                       String title, String artist, double weight) { }

    public final List<Seed> seeds;                    // sorted by weight, highest first
    public final Set<String> excludedSourceKeys;      // "source:sourceId"
    public final Set<String> excludedIsrcs;
    public final Set<String> excludedTitleKeys;       // normalized "artist|title"
    public final Map<String, Double> artistBoost;     // lower(artist) -> multiplier ≥ 1
    public final Map<String, Integer> artistSkips;    // lower(artist) -> recent autoplay skips
    public final Map<String, Integer> artistCounts;   // lower(artist) -> rows in buffer + recent queue

    private ListeningContext(List<Seed> seeds, Set<String> excludedSourceKeys, Set<String> excludedIsrcs,
                             Set<String> excludedTitleKeys, Map<String, Double> artistBoost,
                             Map<String, Integer> artistSkips, Map<String, Integer> artistCounts) {
        this.seeds = seeds;
        this.excludedSourceKeys = excludedSourceKeys;
        this.excludedIsrcs = excludedIsrcs;
        this.excludedTitleKeys = excludedTitleKeys;
        this.artistBoost = artistBoost;
        this.artistSkips = artistSkips;
        this.artistCounts = artistCounts;
    }

    /**
     * Normalizes an artist/title pair into a duplicate-detection key: the same
     * song from different platforms (or with minor punctuation differences)
     * maps to the same key.
     */
    public static String titleKey(String artist, String title) {
        return normalize(artist) + "|" + normalize(title);
    }

    private static String normalize(String value) {
        if (value == null) { return ""; }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)|\\[.*?]", " ")   // (feat. X), [Official Video], ...
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    /**
     * Builds the context for one refill. All queries run on the caller
     * (refill executor) thread.
     *
     * @param playerManager The guild's player.
     * @return The snapshot (possibly with no seeds when the queue is empty).
     */
    public static ListeningContext build(PlayerManager playerManager) {

        long botId = BotIdentity.get();
        long guildId = playerManager.getGuild().getIdLong();
        int cursor = playerManager.getPlayerHandler().getPosition();

        // Who is actually in the voice channel right now — their queued tracks
        // weigh more, and their taste shapes the artist boosts.
        Set<Long> activeListeners = new HashSet<>();
        for (UserRef listener : PlaybackAnalytics.snapshotListeners(playerManager.getGuild())) {
            activeListeners.add(listener.id());
        }

        List<Seed> seeds = new ArrayList<>();
        Set<String> sourceKeys = new HashSet<>();
        Set<String> isrcs = new HashSet<>();
        Set<String> titleKeys = new HashSet<>();
        Map<String, Double> boost = new HashMap<>();
        Map<String, Integer> skips = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        try (Connection connection = DatabaseManager.getConnection()) {

            /* ---- seeds: the tracks around the cursor ---- */

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT qt.position, qt.queued_by, t.id AS track_id, t.source, t.source_id, t.isrc, " +
                    "t.title, t.artist, t.duration_ms " +
                    "FROM queue_tracks qt JOIN tracks t ON t.id = qt.track_id " +
                    "WHERE qt.bot_id = ? AND qt.guild_id = ? AND qt.position BETWEEN ? AND ? " +
                    "ORDER BY qt.position DESC")) {

                statement.setLong(1, botId);
                statement.setLong(2, guildId);
                statement.setInt(3, cursor - SEED_LOOKBACK);
                statement.setInt(4, cursor + SEED_LOOKAHEAD);

                ResultSet result = statement.executeQuery();

                while (result.next()) {

                    // Streams have no duration and make meaningless seeds.
                    result.getLong("duration_ms");
                    if (result.wasNull()) { continue; }

                    int position = result.getInt("position");
                    long queuedBy = result.getLong("queued_by");

                    // Recency: the current track is the strongest seed, each step
                    // back fades. Upcoming user picks count too (they are what the
                    // room wants next) at half strength.
                    double weight = position <= cursor
                            ? Math.pow(0.85, cursor - position)
                            : 0.5 * Math.pow(0.85, position - cursor - 1);

                    // The people in the room right now matter most.
                    if (activeListeners.contains(queuedBy)) { weight *= 1.5; }

                    // A track autoplay picked itself is a weaker signal than one a
                    // human chose.
                    if (queuedBy == botId) { weight *= 0.6; }

                    seeds.add(new Seed(
                            result.getLong("track_id"),
                            result.getString("source"),
                            result.getString("source_id"),
                            result.getString("isrc"),
                            result.getString("title"),
                            result.getString("artist"),
                            weight
                    ));
                }
            }

            seeds.sort((a, b) -> Double.compare(b.weight(), a.weight()));

            /* ---- exclusions: queue + buffer + recent rejections ---- */

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT t.source, t.source_id, t.isrc, t.title, t.artist FROM queue_tracks qt " +
                    "JOIN tracks t ON t.id = qt.track_id WHERE qt.bot_id = ? AND qt.guild_id = ? " +
                    "UNION " +
                    "SELECT t.source, t.source_id, t.isrc, t.title, t.artist FROM autoplay_tracks at " +
                    "JOIN tracks t ON t.id = at.track_id WHERE at.bot_id = ? AND at.guild_id = ? " +
                    "UNION " +
                    "SELECT t.source, t.source_id, t.isrc, t.title, t.artist FROM track_plays tp " +
                    "JOIN tracks t ON t.id = tp.track_id " +
                    "WHERE tp.guild_id = ? AND tp.queued_by IS NULL AND tp.end_reason = 'skipped' " +
                    "AND tp.started_at > now() - interval '" + FEEDBACK_WINDOW + "' " +
                    "UNION " +
                    "SELECT t.source, t.source_id, t.isrc, t.title, t.artist FROM autoplay_feedback af " +
                    "JOIN tracks t ON t.id = af.track_id " +
                    "WHERE af.guild_id = ? AND af.created_at > now() - interval '" + FEEDBACK_WINDOW + "'")) {

                statement.setLong(1, botId);
                statement.setLong(2, guildId);
                statement.setLong(3, botId);
                statement.setLong(4, guildId);
                statement.setLong(5, guildId);
                statement.setLong(6, guildId);

                ResultSet result = statement.executeQuery();

                while (result.next()) {
                    sourceKeys.add(result.getString("source") + ":" + result.getString("source_id"));
                    String isrc = result.getString("isrc");
                    if (isrc != null && !isrc.isBlank()) { isrcs.add(isrc.toUpperCase(Locale.ROOT)); }
                    titleKeys.add(titleKey(result.getString("artist"), result.getString("title")));
                }
            }

            /* ---- artist boosts: what the room's listeners queue on their own ---- */

            if (!activeListeners.isEmpty()) {

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT LOWER(t.artist) AS artist, COUNT(*) AS plays FROM queue_history qh " +
                        "JOIN tracks t ON t.id = qh.track_id " +
                        "WHERE qh.user_id = ANY(?) AND qh.via <> 'autoplay' " +
                        "AND qh.queued_at > now() - interval '" + TASTE_WINDOW + "' " +
                        "GROUP BY 1 ORDER BY plays DESC LIMIT 40")) {

                    statement.setArray(1, connection.createArrayOf("bigint", activeListeners.toArray()));

                    ResultSet result = statement.executeQuery();
                    while (result.next()) {
                        double plays = result.getLong("plays");
                        boost.put(result.getString("artist"), 1.0 + Math.min(0.5, plays / 20.0));
                    }
                }
            }

            /* ---- artist skip penalty: what this guild keeps skipping ---- */

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT LOWER(t.artist) AS artist, COUNT(*) AS skips FROM track_plays tp " +
                    "JOIN tracks t ON t.id = tp.track_id " +
                    "WHERE tp.guild_id = ? AND tp.queued_by IS NULL AND tp.end_reason = 'skipped' " +
                    "AND tp.started_at > now() - interval '" + FEEDBACK_WINDOW + "' " +
                    "GROUP BY 1")) {

                statement.setLong(1, guildId);

                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    skips.put(result.getString("artist"), result.getInt("skips"));
                }
            }

            /* ---- artist saturation: what already fills the buffer + queue tail ---- */

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT LOWER(t.artist) AS artist, COUNT(*) AS n FROM (" +
                    "  SELECT track_id FROM autoplay_tracks WHERE bot_id = ? AND guild_id = ? " +
                    "  UNION ALL " +
                    "  SELECT track_id FROM queue_tracks WHERE bot_id = ? AND guild_id = ? AND position > ? - 10" +
                    ") recent JOIN tracks t ON t.id = recent.track_id GROUP BY 1")) {

                statement.setLong(1, botId);
                statement.setLong(2, guildId);
                statement.setLong(3, botId);
                statement.setLong(4, guildId);
                statement.setInt(5, cursor);

                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    counts.put(result.getString("artist"), result.getInt("n"));
                }
            }

        } catch (Exception ignored) {
            // A failed read just means a thinner context — never break refills.
        }

        return new ListeningContext(seeds, sourceKeys, isrcs, titleKeys, boost, skips, counts);

    }

}
