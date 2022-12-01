package me.nifty.core.database.guild;

import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class GuildHandler {

    private static final Map<Long, Boolean> announcements = new HashMap<>();
    private static final Map<Long, Boolean> inactivityDisconnects = new HashMap<>();

    /**
     * Gets the announcements setting for the specified guild
     *
     * @param guildId The guild to get the announcements setting for
     * @return The announcements setting for the specified guild
     */
    public static Boolean getAnnouncementsMode(long guildId) {

        if (announcements.containsKey(guildId)) {
            return announcements.get(guildId);
        }

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement statement = connection.prepareStatement("SELECT announcements FROM Guilds WHERE guild_id = ? AND announcements IS NOT NULL");
            statement.setLong(1, guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                Boolean announcementsMode = result.getBoolean("announcements");

                announcements.put(guildId, announcementsMode);
                return announcementsMode;
            }

        } catch (Exception e) {
            return true;
        }

        announcements.put(guildId, true);
        return true;

    }

    /**
     * Sets the announcements setting for the specified guild
     *
     * @param guildId The guild to set the announcements setting for
     * @param enabled The new announcements setting for the specified guild
     */
    public static void setAnnouncementsMode(long guildId, boolean enabled) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement updateStatement = connection.prepareStatement("INSERT INTO Guilds (guild_id, announcements) VALUES (?, ?) ON DUPLICATE KEY UPDATE announcements = ?");
            updateStatement.setLong(1, guildId);
            updateStatement.setBoolean(2, enabled);
            updateStatement.setBoolean(3, enabled);

            int updateResult = updateStatement.executeUpdate();

            if (updateResult > 0) {
                announcements.put(guildId, enabled);
            }

        } catch (Exception ignored) { }

    }

    /**
     * Gets the inactivity disconnect setting for the specified guild
     *
     * @param guildId The guild to get the inactivity disconnect setting for
     * @return The inactivity disconnect setting for the specified guild
     */
    public static Boolean getInactivityDisconnect(long guildId) {

        if (inactivityDisconnects.containsKey(guildId)) {
            return inactivityDisconnects.get(guildId);
        }

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement statement = connection.prepareStatement("SELECT inactivity_disconnect FROM Guilds WHERE guild_id = ? AND inactivity_disconnect IS NOT NULL");
            statement.setLong(1, guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                Boolean inactivityDisconnect = result.getBoolean("inactivity_disconnect");

                inactivityDisconnects.put(guildId, inactivityDisconnect);
                return inactivityDisconnect;
            }

        } catch (Exception e) {
            return false;
        }

        inactivityDisconnects.put(guildId, false);
        return false;

    }

    /**
     * Sets the inactivity disconnect setting for the specified guild
     *
     * @param guildId The guild to set the inactivity disconnect setting for
     * @param enabled The new inactivity disconnect setting for the specified guild
     */
    public static void setInactivityDisconnect(long guildId, boolean enabled) {

        Connection connection = DatabaseManager.getConnection();

        try {

            PreparedStatement updateStatement = connection.prepareStatement("INSERT INTO Guilds (guild_id, inactivity_disconnect) VALUES (?, ?) ON DUPLICATE KEY UPDATE inactivity_disconnect = ?");
            updateStatement.setLong(1, guildId);
            updateStatement.setBoolean(2, enabled);
            updateStatement.setBoolean(3, enabled);

            int updateResult = updateStatement.executeUpdate();

            if (updateResult > 0) {
                inactivityDisconnects.put(guildId, enabled);
            }

        } catch (Exception ignored) { }

    }

}
