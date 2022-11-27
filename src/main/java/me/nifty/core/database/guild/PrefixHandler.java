package me.nifty.core.database.guild;

import me.nifty.Config;
import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class PrefixHandler {

    private static final String defaultPrefix = Config.getDefaultPrefix();
    private static final Map<Long, String> prefixes = new HashMap<>();

    public static String getPrefix(long guildId) {

        Connection connection = DatabaseManager.getConnection();

        if (prefixes.containsKey(guildId)) {
            return prefixes.get(guildId);
        }

        try {

            PreparedStatement statement = connection.prepareStatement("SELECT prefix FROM Guilds WHERE guild_id = ? AND prefix IS NOT NULL");
            statement.setLong(1, guildId);

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

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement updateStatement = connection.prepareStatement("INSERT INTO Guilds (guild_id, prefix) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefix = ?");
            updateStatement.setLong(1, guildId);
            updateStatement.setString(2, newPrefix);
            updateStatement.setString(3, newPrefix);

            int updateResult = updateStatement.executeUpdate();

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
