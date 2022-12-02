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
     * Gets the SQLite url
     * @return The SQLite url
     */
    public static String getSQLiteUrl() {
        return config.get("SQLITE_URL");
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
