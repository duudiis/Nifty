package me.nifty.core.music.managers;

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.websocket.payloads.WsUpdates;

import java.util.List;

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

    public AudioFiltersManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.audioPlayer = playerManager.getAudioPlayer();
        this.playerHandler = playerManager.getPlayerHandler();
    }

    public void updateFilterFactory() {

        if (!areFiltersEnabled()) {
            audioPlayer.setFilterFactory(null);
            return;
        }

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

    public boolean areFiltersEnabled() {
        return getSpeed() != 1.0f || getPitch() != 1.0f || getBassBoost() != 0.0f || getRotation();
    }

    public float getSpeed() {
        return playerHandler.getSpeed();
    }

    public void setSpeed(float speed) {
        playerHandler.setSpeed(speed);
        updateFilterFactory();

        // Playback advances at the new rate from here on: re-anchor the
        // wall-clock position so database readers derive progress correctly,
        // and tell the dashboard the player changed.
        AudioTrack playingTrack = audioPlayer.getPlayingTrack();
        if (playingTrack != null) {
            playerHandler.anchorPosition(playingTrack.getPosition());
        }
        WsUpdates.player(playerManager);
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
