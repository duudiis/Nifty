package me.nifty.managers;

import me.nifty.Config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    private static Connection connection;

    private static final String host = Config.getMySQLHost();
    private static final String port = Config.getMySQLPort();
    private static final String database = Config.getMySQLDatabase();
    private static final String username = Config.getMySQLUser();
    private static final String password = Config.getMySQLPassword();


    /**
     * Connects to the MySQL Server
     */
    public static void connect() {

        // Checks if the credentials are missing
        if (host == null || port == null || username == null || password == null) {
            throw new RuntimeException("[Nifty] MySQL credentials missing on the environment variables!");
        }

        // Attempts to connect to the MySQL Server
        System.out.println("[Nifty] Attempting to connect to MySQL Server...");

        try {
            //connection = DriverManager.getConnection(("jdbc:mysql://" + host + ":" + port + "/" + database), username, password);
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
            createTables(connection);
            //System.out.println("[Nifty] Successfully connected to MySQL Server!");
        } catch (Exception e) {
            throw new RuntimeException("[Nifty] Failed to connect to MySQL Server with Error:\n", e);
        }

    }

    /**
     * Gets the connection
     * @return The connection
     */
    public static Connection getConnection() {
        return connection;
    }

    /**
     * Creates the tables, if it is a new database
     * @param connection The connection
     */
    private static void createTables(Connection connection) {

        try {

            String guilds = "CREATE TABLE IF NOT EXISTS Guilds (" +
                    "guild_id VARCHAR(32) NOT NULL UNIQUE," +
                    "prefix VARCHAR(16)," +
                    "inactivity_disconnect BOOLEAN," +
                    "announcements VARCHAR(32)," +
                    "PRIMARY KEY (guild_id)" +
                    ");";

            String perms = "CREATE TABLE IF NOT EXISTS Perms (" +
                    "guild_id VARCHAR(32) NOT NULL UNIQUE," +
                    "entity_id VARCHAR(32) NOT NULL," +
                    "entity_type INT NOT NULL," +
                    "permission INT NOT NULL," +
                    "PRIMARY KEY (guild_id)" +
                    ");";

            String players = "CREATE TABLE IF NOT EXISTS Players (" +
                    "guild_id VARCHAR(32) NOT NULL UNIQUE," +
                    "channel_id VARCHAR(32)," +
                    "voice_id VARCHAR(32)," +
                    "position INT NOT NULL," +
                    "playing BOOLEAN NOT NULL," +
                    "looping VARCHAR(32)," +
                    "shuffle VARCHAR(32)," +
                    "autoplay VARCHAR(32)," +
                    "speed FLOAT," +
                    "pitch FLOAT," +
                    "bass_boost FLOAT," +
                    "rotation BOOLEAN," +
                    "PRIMARY KEY (guild_id)" +
                    ");";

            String queues = "CREATE TABLE IF NOT EXISTS Queues (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "guild_id VARCHAR(32) NOT NULL," +
                    "track_id INT NOT NULL," +
                    "track_name VARCHAR(256) NOT NULL," +
                    "member_id VARCHAR(32) NOT NULL," +
                    "encoded_track VARCHAR(1024) NOT NULL" +
                    ");";

            connection.createStatement().execute(guilds);
            connection.createStatement().execute(perms);
            connection.createStatement().execute(players);
            connection.createStatement().execute(queues);

        } catch (Exception e) {
            throw new RuntimeException("[Nifty] Failed to create tables with Error:\n", e);
        }

    }

}
