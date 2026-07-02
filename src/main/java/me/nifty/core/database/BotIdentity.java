package me.nifty.core.database;

import me.nifty.Config;
import me.nifty.managers.DatabaseManager;
import net.dv8tion.jda.api.entities.SelfUser;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * This bot instance's identity in the shared database.
 *
 * <p>Several bot accounts (Nifty, Nifty 2, ...) share one PostgreSQL server:
 * players, queues and guild settings are scoped by {@code bot_id} — this bot's
 * own Discord user id. The instance registers itself in the {@code bots} table
 * on ready, so adding another instance needs no database setup at all.</p>
 */
public class BotIdentity {

    private static volatile long botId = 0;

    /**
     * Registers this bot instance in the database. Called once on ready.
     *
     * @param selfUser The bot's own Discord user
     */
    public static void initialize(SelfUser selfUser) {

        botId = selfUser.getIdLong();

        try (Connection connection = DatabaseManager.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bots (id, name) VALUES (?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name")) {
                statement.setLong(1, botId);
                statement.setString(2, Config.getDashboardBotName());
                statement.executeUpdate();
            }

            // The bot is also a user: autoplay attributes queued tracks to it.
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (id, username, display_name, avatar_url) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, " +
                    "display_name = EXCLUDED.display_name, avatar_url = EXCLUDED.avatar_url")) {
                statement.setLong(1, botId);
                statement.setString(2, selfUser.getName());
                statement.setString(3, selfUser.getName());
                statement.setString(4, selfUser.getEffectiveAvatarUrl());
                statement.executeUpdate();
            }

        } catch (Exception e) {
            System.out.println("[Nifty] Failed to register the bot instance in the database: " + e.getMessage());
        }

    }

    /**
     * @return This bot instance's id (its Discord user id), or 0 before ready.
     */
    public static long get() {
        return botId;
    }

}
