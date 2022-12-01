package me.nifty.utils.parser;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TrackParser {

    private static final Pattern literalIntegerPattern = Pattern.compile("^[0-9]+$");
    private static final Pattern relativeIntegerPattern = Pattern.compile("^[+-][0-9]+$");

    private static final Map<String, List<String>> keywords = Map.of(
            "first", List.of("f", "first"),
            "back", List.of("b", "back", "p", "previous"),
            "current", List.of("c", "current", "t", "this", "n", "now", "np", "nowplaying", "playing"),
            "next", List.of("n", "next", "skip"),
            "last", List.of("l", "last", "end", "final")
    );

    /**
     * Parses a string to determine if it is a valid track, accepts a string with the number
     * of the track on the queue, a string with a relative number of tracks to the currently playing
     * track, a keyword for the first, last, current, next or last track, or a string with the
     * title of the track.
     *
     * @param query The string to parse.
     * @param playerManager The player manager to use to get the queue.
     * @return The track number if the string is a valid track number, otherwise -1.
     */
    public static int parse(String query, PlayerManager playerManager) {

        // Checks if the query is a literal integer.
        if (query.matches(literalIntegerPattern.pattern())) {

            int trackNumber = Integer.parseInt(query) - 1;

            AudioTrack track = playerManager.getQueueHandler().getQueueTrack(trackNumber);

            if (track != null) {
                return trackNumber;
            } else {
                return -1;
            }

        }

        // Checks if the query is a relative integer.
        if (query.matches(relativeIntegerPattern.pattern())) {

            int position = playerManager.getPlayerHandler().getPosition();

            int trackNumber = position + Integer.parseInt(query);

            AudioTrack track = playerManager.getQueueHandler().getQueueTrack(trackNumber);

            if (track != null) {
                return trackNumber;
            } else {
                return -1;
            }

        }

        // Checks if the query is a keyword.
        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            if (entry.getValue().contains(query.toLowerCase())) {
                switch (entry.getKey()) {
                    case "first" -> {
                        return 0;
                    }
                    case "back" -> {
                        return Math.max(playerManager.getPlayerHandler().getPosition() - 1, 0);
                    }
                    case "current" -> {
                        return playerManager.getPlayerHandler().getPosition();
                    }
                    case "next" -> {
                        return playerManager.getPlayerHandler().getPosition() + 1;
                    }
                    case "last" -> {
                        return playerManager.getQueueHandler().getQueueSize() - 1;
                    }
                }
            }
        }

        // Checks if the query is a track title.
        return playerManager.getQueueHandler().searchQueueTrack(query);

    }

}
