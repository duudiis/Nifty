package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackTitle {

    private static final List<String> sourcesWithArtist = List.of("spotify", "deezer", "applemusic");
    private static final List<String> disallowedParenthesesKeywords = List.of("video", "lyric", "audio");

    /**
     * Formats the title of a track to a specified length, also adds the artist if the source
     * of the track is Spotify, Deezer or Apple Music.
     *
     * @param audioTrack The track to format.
     * @param maxLength The length to format the title to.
     * @return The formatted title.
     */
    public static String format(AudioTrack audioTrack, int maxLength) {

        // Gets the source manager of the track
        String trackSource = audioTrack.getSourceManager().getSourceName();

        // Gets the title of the track
        String trackTitle = audioTrack.getInfo().title;

        // If the source manager requires the artist to be added to the title
        if (sourcesWithArtist.contains(trackSource) || (!trackTitle.contains(" - ") && !trackTitle.contains(audioTrack.getInfo().author))) {

            // Adds the artist to the title
            trackTitle = audioTrack.getInfo().author + " - " + trackTitle;

        }

        // Replace "- Topic" from the title
        trackTitle = trackTitle.replaceAll(" - Topic", "");

        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(trackTitle);

        while (m.find()) {

            String parentheses = m.group(1);

            for (String keyword : disallowedParenthesesKeywords) {

                if (parentheses.toLowerCase().contains(keyword)) {

                    trackTitle = trackTitle.replace("(" + parentheses + ")", "");

                }

            }

        }

        trackTitle = trackTitle.replaceAll("\\s+", " ");
        trackTitle = trackTitle.trim().replaceAll("^-|-$", "");

        // If the title is longer than the specified length
        if (trackTitle.length() > maxLength) {

            // Shortens the title to the specified length and adds 3 dots to the end
            return trackTitle.substring(0, maxLength).trim() + "\u2026";

        } else {

            // Returns the title
            return trackTitle;

        }

    }

}