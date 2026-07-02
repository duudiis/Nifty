package me.nifty.core.database.guild;

import me.nifty.Config;
import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.GuildStore;
import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The command prefix per guild for this bot instance ({@code guild_settings},
 * keyed by (bot_id, guild_id)). NULL means the default prefix.
 */
public class PrefixHandler {

    private static final String defaultPrefix = Config.getDefaultPrefix();
    private static final Map<Long, String> prefixes = new HashMap<>();

    public static String getPrefix(long guildId) {

        if (prefixes.containsKey(guildId)) {
            return prefixes.get(guildId);
        }

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT prefix FROM guild_settings WHERE bot_id = ? AND guild_id = ? AND prefix IS NOT NULL")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String prefix = result.getString("prefix");

                prefixes.put(guildId, prefix);
                return prefix;
            }

        } catch (Exception e) {
            return defaultPrefix;
        }

        prefixes.put(guildId, defaultPrefix);
        return defaultPrefix;

    }

    public static boolean setPrefix(long guildId, String newPrefix) {

        GuildStore.ensure(guildId);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guild_settings (bot_id, guild_id, prefix) VALUES (?, ?, ?) " +
                     "ON CONFLICT (bot_id, guild_id) DO UPDATE SET prefix = EXCLUDED.prefix")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);
            statement.setString(3, newPrefix);

            int updateResult = statement.executeUpdate();

            if (updateResult > 0) {
                prefixes.put(guildId, newPrefix);
                return true;
            } else {
                return false;
            }

        } catch (Exception ignored) { }

        return false;

    }

}
