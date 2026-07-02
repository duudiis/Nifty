package me.nifty.core.database;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.DatabaseManager;
import me.nifty.utils.TrackUtils;
import me.nifty.utils.formatting.Artwork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The canonical track catalog: every queue entry, history row and play refers
 * to a {@code tracks} row, deduplicated by (source, source_id).
 *
 * <p>The lavaplayer-encoded blob is stored alongside the metadata as a
 * playback cache, refreshed on every upsert so it always matches the current
 * lavaplayer version.</p>
 */
public class TrackStore {

    private static final int CACHE_LIMIT = 10_000;

    private static final Map<String, Long> idCache = new ConcurrentHashMap<>();

    /**
     * Ensures the track exists in the catalog and returns its id.
     *
     * @param track The lavaplayer track to store
     * @return The catalog id, or -1 if the track could not be stored
     */
    public static long upsert(AudioTrack track) {

        String source = track.getSourceManager() != null ? track.getSourceManager().getSourceName() : "unknown";
        String sourceId = track.getIdentifier();

        String cacheKey = source + ":" + sourceId;
        Long cached = idCache.get(cacheKey);
        if (cached != null) { return cached; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tracks (source, source_id, title, artist, duration_ms, url, artwork_url, isrc, encoded) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (source, source_id) DO UPDATE SET title = EXCLUDED.title, artist = EXCLUDED.artist, " +
                     "duration_ms = EXCLUDED.duration_ms, url = EXCLUDED.url, artwork_url = EXCLUDED.artwork_url, " +
                     "isrc = COALESCE(EXCLUDED.isrc, tracks.isrc), encoded = EXCLUDED.encoded " +
                     "RETURNING id")) {

            statement.setString(1, source);
            statement.setString(2, sourceId);
            statement.setString(3, track.getInfo().title);
            statement.setString(4, track.getInfo().author);

            if (track.getInfo().isStream) {
                statement.setNull(5, Types.BIGINT);
            } else {
                statement.setLong(5, track.getDuration());
            }

            statement.setString(6, track.getInfo().uri);
            statement.setString(7, Artwork.get(track));
            statement.setString(8, track.getInfo().isrc);
            statement.setString(9, TrackUtils.encodeTrack(track));

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                long id = result.getLong(1);

                if (idCache.size() >= CACHE_LIMIT) { idCache.clear(); }
                idCache.put(cacheKey, id);

                return id;
            }

        } catch (Exception ignored) {
            // Catalog writes must never break playback paths.
        }

        return -1;

    }

}
