package me.nifty.managers;

import me.nifty.Config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    private static Connection connection;

    private static final String url = Config.getMySQLUrl();
    private static final String username = Config.getMySQLUsername();
    private static final String password = Config.getMySQLPassword();

    /**
     * Connects to the MySQL Server
     */
    public static void connect() {

        // Checks if the credentials are missing
        if (url == null || username == null || password == null) {
            throw new RuntimeException("[Nifty] MySQL credentials missing on the environment variables!");
        }

        // Attempts to connect to the MySQL Server
        System.out.println("[Nifty] Attempting to connect to MySQL Server...");

        try {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("[Nifty] Successfully connected to MySQL Server!");
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

}
