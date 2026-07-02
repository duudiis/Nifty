package me.nifty.core.database;

import me.nifty.managers.DatabaseManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Upserts Discord users into the shared {@code users} table.
 *
 * <p>Every row that references a user (queue entries, history, listening
 * segments, ...) needs the user to exist first. Profile data is a cache —
 * refreshed at most once per {@link #REFRESH_MS} per user to avoid hammering
 * the database with identical upserts.</p>
 */
public class UserStore {

    private static final long REFRESH_MS = 10 * 60 * 1000;

    private static final Map<Long, Long> lastUpsert = new ConcurrentHashMap<>();

    /**
     * A minimal, thread-safe snapshot of a Discord user, taken on the event
     * thread so database work never has to touch JDA entities.
     */
    public record UserRef(long id, String username, String displayName, String avatarUrl) {

        public static UserRef of(Member member) {
            return new UserRef(
                    member.getIdLong(),
                    member.getUser().getName(),
                    member.getEffectiveName(),
                    member.getEffectiveAvatarUrl()
            );
        }

        public static UserRef of(User user) {
            return new UserRef(user.getIdLong(), user.getName(), user.getEffectiveName(), user.getEffectiveAvatarUrl());
        }

    }

    /**
     * Ensures a bare user row exists so foreign keys hold, without touching
     * profile data. Used on hot paths where only the id is at hand; the full
     * profile arrives later via {@link #upsert(UserRef)}.
     *
     * @param userId The user id to ensure
     */
    public static void ensureExists(long userId) {

        if (lastUpsert.containsKey(userId)) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (id) VALUES (?) ON CONFLICT (id) DO NOTHING")) {

            statement.setLong(1, userId);
            statement.executeUpdate();

            // Mark as existing but stale, so the next profile upsert still runs.
            lastUpsert.putIfAbsent(userId, 0L);

        } catch (Exception ignored) {
            // User anchoring must never break the caller.
        }

    }

    /**
     * Ensures the user exists and refreshes their cached profile.
     *
     * @param user The user snapshot to upsert
     */
    public static void upsert(UserRef user) {

        long now = System.currentTimeMillis();
        Long previous = lastUpsert.get(user.id());
        if (previous != null && (now - previous) < REFRESH_MS) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (id, username, display_name, avatar_url, last_seen_at) VALUES (?, ?, ?, ?, now()) " +
                     "ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, " +
                     "display_name = EXCLUDED.display_name, avatar_url = EXCLUDED.avatar_url, last_seen_at = now()")) {

            statement.setLong(1, user.id());
            statement.setString(2, user.username());
            statement.setString(3, user.displayName());
            statement.setString(4, user.avatarUrl());

            statement.executeUpdate();
            lastUpsert.put(user.id(), now);

        } catch (Exception ignored) {
            // User profile caching must never break the caller.
        }

    }

}
