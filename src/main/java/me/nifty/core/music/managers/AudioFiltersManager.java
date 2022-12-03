package me.nifty.core.music.managers;

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.music.PlayerManager;

import java.util.List;

public class AudioFiltersManager {

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

    public AudioFiltersManager(PlayerManager playerManager) {
        this.audioPlayer = playerManager.getAudioPlayer();
        this.playerHandler = playerManager.getPlayerHandler();
    }

    public void updateFilterFactory() {

        float speed = getSpeed();
        float pitch = getPitch();
        float bassBoost = getBassBoost();
        boolean rotationIsEnabled = getRotation();

        audioPlayer.setFilterFactory((track, format, output) -> {

            TimescalePcmAudioFilter timescale = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);

            timescale.setSpeed(speed);
            timescale.setPitch(pitch);

            Equalizer equalizer = new Equalizer(format.channelCount, timescale);

            for (int i = 0; i < BASS_BOOST.length; i++) {
                equalizer.setGain(i, BASS_BOOST[i] * bassBoost);
            }

            if (rotationIsEnabled) {
                RotationPcmAudioFilter rotation = new RotationPcmAudioFilter(equalizer, format.sampleRate);
                rotation.setRotationSpeed(0.1);

                return List.of(rotation, equalizer, timescale);
            }

            return List.of(equalizer, timescale);

        });

    }

    public float getSpeed() {
        return playerHandler.getSpeed();
    }

    public void setSpeed(float speed) {
        playerHandler.setSpeed(speed);
        updateFilterFactory();
    }

    public float getPitch() {
        return playerHandler.getPitch();
    }

    public void setPitch(float pitch) {
        playerHandler.setPitch(pitch);
        updateFilterFactory();
    }

    public float getBassBoost() {
        return playerHandler.getBassBoost();
    }

    public void setBassBoost(float bassBoost) {
        playerHandler.setBassBoost(bassBoost);
        updateFilterFactory();
    }

    public boolean getRotation() {
        return playerHandler.getRotation();
    }

    public void setRotation(boolean rotation) {
        playerHandler.setRotation(rotation);
        updateFilterFactory();
    }

}
