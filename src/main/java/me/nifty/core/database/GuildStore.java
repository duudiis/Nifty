package me.nifty.core.database;

import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ensures guild entity rows exist in the shared {@code guilds} table.
 * The table is a pure anchor for foreign keys — one row per guild, shared
 * across bot instances.
 */
public class GuildStore {

    private static final Set<Long> known = ConcurrentHashMap.newKeySet();

    /**
     * Makes sure the guild row exists. Cheap after the first call per guild.
     *
     * @param guildId The guild to ensure
     */
    public static void ensure(long guildId) {

        if (known.contains(guildId)) { return; }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guilds (id) VALUES (?) ON CONFLICT (id) DO NOTHING")) {

            statement.setLong(1, guildId);
            statement.executeUpdate();
            known.add(guildId);

        } catch (Exception ignored) {
            // Guild anchoring must never break the caller.
        }

    }

}
