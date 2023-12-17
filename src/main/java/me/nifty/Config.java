package me.nifty;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private static final Dotenv config = Dotenv.load();

    /**
     * Gets the discord token
     * @return The discord token
     */
    public static String getDiscordToken() {
        return config.get("DISCORD_TOKEN");
    }

    /**
     * Gets the default prefix
     * @return The default prefix
     */
    public static String getDefaultPrefix() {
        return config.get("DISCORD_DEFAULT_PREFIX");
    }

    /**
     * Gets the MySQL host
     * @return The MySQL url
     */
    public static String getMySQLHost() {
        return config.get("MYSQL_HOST");
    }

    /**
     * Gets the MySQL port
     * @return The MySQL port
     */
    public static String getMySQLPort() {
        return config.get("MYSQL_PORT");
    }

    /**
     * Gets the MySQL user
     * @return The MySQL user
     */
    public static String getMySQLUser() {
        return config.get("MYSQL_USER");
    }

    /**
     * Gets the MySQL password
     * @return The MySQL password
     */
    public static String getMySQLPassword() {
        return config.get("MYSQL_PASSWORD");
    }

    /**
     * Gets the MySQL database
     * @return The MySQL database
     */
    public static String getMySQLDatabase() {
        return config.get("MYSQL_DATABASE");
    }

    /**
     * Gets the Spotify client id
     * @return The Spotify client id
     */
    public static String getSpotifyClientId() {
        return config.get("SPOTIFY_CLIENT_ID");
    }

    /**
     * Gets the Spotify client secret
     * @return The Spotify client secret
     */
    public static String getSpotifyClientSecret() {
        return config.get("SPOTIFY_CLIENT_SECRET");
    }

    /**
     * Gets the Deezer master decryption key
     * @return The Deezer master decryption key
     */
    public static String getDeezerMasterDecryptionKey() {
        return config.get("DEEZER_MASTER_DECRYPTION_KEY");
    }

}
