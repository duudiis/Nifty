package me.nifty.managers;

import me.nifty.Config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class DatabaseManager {

    private static Connection connection;

    private static final String url = Config.getSQLiteUrl();

    /**
     * Connects to the MySQL Server
     */
    public static void connect() {

        // Checks if the credentials are missing
        if (url == null) {
            throw new RuntimeException("[Nifty] SQL URL missing on the environment variables!");
        }

        // Attempts to connect to the SQLite Server
        System.out.println("[Nifty] Attempting to connect to SQLite Server...");

        try {
            connection = DriverManager.getConnection(url);
            System.out.println("[Nifty] Successfully connected to SQLite Server!");
        } catch (Exception e) {
            throw new RuntimeException("[Nifty] Failed to connect to SQLite Server with Error:\n", e);
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
