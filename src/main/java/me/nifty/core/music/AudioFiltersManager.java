package me.nifty.core.music;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.nifty.core.database.music.PlayerHandler;

import java.util.Arrays;

public class AudioFiltersManager {

    private final PlayerManager playerManager;
    private final AudioPlayer audioPlayer;
    private final PlayerHandler playerHandler;

    public static final float[] BASS_BOOST = {
            0.2f,
            0.15f,
            0.1f,
            0.05f,
            0.0f,
            -0.05f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f
    };

    private float speed = 1;
    private float pitch = 1;
    private float bassBoost = 0;

    public AudioFiltersManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.audioPlayer = playerManager.getAudioPlayer();
        this.playerHandler = playerManager.getPlayerHandler();
    }

    public void updateFilterFactory() {

        audioPlayer.setFilterFactory((track, format, output) -> {

            TimescalePcmAudioFilter timescale = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);

            timescale.setSpeed(speed);
            timescale.setPitch(pitch);

            Equalizer equalizer = new Equalizer(format.channelCount, timescale);

            for (int i = 0; i < BASS_BOOST.length; i++)
            {
                equalizer.setGain(i, BASS_BOOST[i] * bassBoost);
            }

            return Arrays.asList(equalizer, timescale);
        });

    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        updateFilterFactory();
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        updateFilterFactory();
    }

    public float getBassBoost() {
        return bassBoost;
    }

    public void setBassBoost(float bassBoost) {
        this.bassBoost = bassBoost;
        updateFilterFactory();
    }

}
