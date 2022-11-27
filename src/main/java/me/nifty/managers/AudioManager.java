package me.nifty.managers;

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topisenpai.lavasrc.deezer.DeezerAudioSourceManager;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import me.nifty.Config;

public class AudioManager {

    private static AudioPlayerManager audioManager;

    /**
     * Loads the audio manager.
     */
    public static void load() {

        // Create a new audio player manager.
        audioManager = new DefaultAudioPlayerManager();

        // Sets the number of threads to use for loading items.
        audioManager.setItemLoaderThreadPoolSize(128);

        // Sets the resampling quality.
        audioManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        // Allows the filters to be swapped without restarting the player.
        audioManager.getConfiguration().setFilterHotSwapEnabled(true);

        // Sets the duration of the frame buffer to 500 so audio filters changes are more responsive.
        audioManager.setFrameBufferDuration(500);

        // Sets ghost seeking to false so the player stops while the track is loading.
        audioManager.setUseSeekGhosting(false);

        // Registers internal source managers.
        audioManager.registerSourceManager(new YoutubeAudioSourceManager());
        audioManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());

        audioManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioManager.registerSourceManager(new VimeoAudioSourceManager());
        audioManager.registerSourceManager(new BandcampAudioSourceManager());

        // Registers external source managers.
        audioManager.registerSourceManager(new AppleMusicSourceManager(null, "", "us", audioManager));

        // Spotify requires a client id and secret to be set.
        String spotifyClientId = Config.getSpotifyClientId();
        String spotifyClientSecret = Config.getSpotifyClientSecret();

        if (spotifyClientId != null && spotifyClientSecret != null) {
            audioManager.registerSourceManager(new SpotifySourceManager(null, Config.getSpotifyClientId(), Config.getSpotifyClientSecret(), "US", audioManager));
        }

        // Deezer requires a master decryption key to be set.
        String deezerMasterDecryptionKey = Config.getDeezerMasterDecryptionKey();

        if (deezerMasterDecryptionKey != null) {
            audioManager.registerSourceManager(new DeezerAudioSourceManager(deezerMasterDecryptionKey));
        }

        // Registers the HTTP source manager.
        audioManager.registerSourceManager(new HttpAudioSourceManager());

    }

    /**
     * Gets the audio manager.
     *
     * @return The audio player manager.
     */
    public static AudioPlayerManager getAudioManager() {
        return audioManager;
    }

}
