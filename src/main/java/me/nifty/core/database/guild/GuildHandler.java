package me.nifty.core.database.guild;

import me.nifty.core.database.BotIdentity;
import me.nifty.core.database.GuildStore;
import me.nifty.managers.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-guild settings for this bot instance, stored in {@code guild_settings}
 * keyed by (bot_id, guild_id). A NULL column means "not configured" and the
 * code default applies.
 */
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

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT announcements FROM guild_settings WHERE bot_id = ? AND guild_id = ? AND announcements IS NOT NULL")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                boolean announcementsMode = result.getBoolean("announcements");

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

        GuildStore.ensure(guildId);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guild_settings (bot_id, guild_id, announcements) VALUES (?, ?, ?) " +
                     "ON CONFLICT (bot_id, guild_id) DO UPDATE SET announcements = EXCLUDED.announcements")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);
            statement.setBoolean(3, enabled);

            int updateResult = statement.executeUpdate();

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

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT inactivity_disconnect FROM guild_settings WHERE bot_id = ? AND guild_id = ? AND inactivity_disconnect IS NOT NULL")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                boolean inactivityDisconnect = result.getBoolean("inactivity_disconnect");

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

        GuildStore.ensure(guildId);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guild_settings (bot_id, guild_id, inactivity_disconnect) VALUES (?, ?, ?) " +
                     "ON CONFLICT (bot_id, guild_id) DO UPDATE SET inactivity_disconnect = EXCLUDED.inactivity_disconnect")) {

            statement.setLong(1, BotIdentity.get());
            statement.setLong(2, guildId);
            statement.setBoolean(3, enabled);

            int updateResult = statement.executeUpdate();

            if (updateResult > 0) {
                inactivityDisconnects.put(guildId, enabled);
            }

        } catch (Exception ignored) { }

    }

}
