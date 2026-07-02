package me.nifty.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.nifty.Config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool for the shared PostgreSQL server.
 *
 * <p>The schema is owned by the SQL migrations in {@code migrations/} and is
 * never created from code. Callers must close every connection they take —
 * always use try-with-resources; connections return to the pool on close.</p>
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;

    /**
     * Opens the connection pool against the configured PostgreSQL server.
     */
    public static void connect() {

        String url = Config.getDatabaseUrl();

        if (url == null || url.isBlank()) {
            throw new RuntimeException("[Nifty] DATABASE_URL missing on the environment variables!");
        }

        System.out.println("[Nifty] Attempting to connect to PostgreSQL...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(Config.getDatabaseUser());
        config.setPassword(Config.getDatabasePassword());
        config.setPoolName("nifty-db");
        config.setMaximumPoolSize(6);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setKeepaliveTime(300_000);
        config.setMaxLifetime(1_800_000);

        try {
            dataSource = new HikariDataSource(config);

            // Fail fast on boot if the server is unreachable or credentials are wrong.
            try (Connection connection = dataSource.getConnection()) {
                connection.isValid(5);
            }

            System.out.println("[Nifty] Successfully connected to PostgreSQL!");
        } catch (Exception e) {
            throw new RuntimeException("[Nifty] Failed to connect to PostgreSQL with Error:\n", e);
        }

    }

    /**
     * Borrows a connection from the pool. The caller must close it
     * (try-with-resources) to return it to the pool.
     *
     * @return A pooled connection
     * @throws SQLException If no connection is available
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
