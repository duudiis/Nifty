package me.nifty.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.AudioManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class TrackUtils {

    private static final AudioPlayerManager audioManager = AudioManager.getAudioManager();

    /**
     * Encodes an AudioTrack to a Base64 String
     *
     * @param track The AudioTrack to encode
     * @return The Base64 String
     */
    @Nullable
    public static String encodeTrack(AudioTrack track) {

        try {

            // Create a new ByteArrayOutputStream
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            // Encodes the AudioTrack to the ByteArrayOutputStream
            audioManager.encodeTrack(new MessageOutput(stream), track);

            // Returns the Base64 String
            byte[] encoded = Base64.getEncoder().encode(stream.toByteArray());

            return new String(encoded);

        } catch (Exception ignored) { }

        return null;

    }

    /**
     * Decodes a Base64 String to an AudioTrack
     *
     * @param encodedString The Base64 String to decode
     * @return The decoded AudioTrack
     */
    @Nullable
    public static AudioTrack decodeTrack(String encodedString) {

        try {

            // Decodes the Base64 String
            byte[] decoded = Base64.getDecoder().decode(encodedString);

            // Creates a new ByteArrayInputStream with the decoded Base64 String
            ByteArrayInputStream stream = new ByteArrayInputStream(decoded);

            // Returns the decoded AudioTrack
            return audioManager.decodeTrack(new MessageInput(stream)).decodedTrack;

        } catch (Exception ignored) { }

        return null;

    }

}
