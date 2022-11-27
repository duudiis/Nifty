package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;

import java.util.List;

public class QueueMessage {

    /**
     * Formats the queue page to a string to be sent in a message.
     *
     * @param playerManager The player manager to use to get the queue.
     * @param page The page of the queue to format.
     * @return The formatted queue.
     */
    public static String get(PlayerManager playerManager, int page) {

        // Gets the tracks from this page of the queue
        List<AudioTrack> queueTracks = playerManager.getQueueHandler().getQueuePage(page);

        // If there are no tracks on this page, something went wrong
        if (queueTracks == null) { return "```nim\nUnknown Page\n```"; }

        // Creates the string builder to build the message
        StringBuilder queueMessage = new StringBuilder();
        queueMessage.append("```nim\n");

        // Gets the first track number on this page

        int trackNumber = page * 10;

        int queueSize = playerManager.getQueueHandler().getQueueSize();
        int pageMax = (queueSize % 10 == 0) ? ((queueSize / 10) - 1) : (queueSize / 10);

        if (queueSize > 10 && page == pageMax) {
            trackNumber = -(10 - queueSize);
        }

        // Gets the padding for the track numbers
        int startPadding = String.valueOf((trackNumber + Math.min(queueSize, 10))).length();

        // Gets the current playing track
        AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();

        // Loops through the tracks on this page to add them to the message
        for (AudioTrack track : queueTracks) {
            trackNumber++;

            // Formats the track title
            String title = TrackTitle.format(track, 36);

            // Checks if this track is the currently playing track
            if ((trackNumber - 1) == playerManager.getPlayerHandler().getPosition() && playingTrack != null) {

                // Adds the track number, title and duration to the message as the currently playing track

                String timeLeft = TrackTime.formatClock((track.getDuration() - playingTrack.getPosition()));

                queueMessage.append(String.format("%" + (startPadding + 3) + "s", "")).append("\u2B10 current track\n");

                queueMessage.append(String.format("%" + startPadding + "s", trackNumber)).append(") ");
                queueMessage.append(String.format("%-" + 37 + "s", title)).append("  ").append(timeLeft).append(" left");
                queueMessage.append("\n");

                queueMessage.append(String.format("%" + (startPadding + 3) + "s", "")).append("\u2B11 current track\n");

                continue;
            }

            // Adds the track number, title and duration to the message

            String time = TrackTime.formatClock(track.getDuration());

            queueMessage.append(String.format("%" + startPadding + "s", trackNumber)).append(") ");
            queueMessage.append(String.format("%-" + 37 + "s", title)).append("  ").append(time);
            queueMessage.append("\n");

        }

        // Gets the ending of the message

        queueMessage.append("\n").append(String.format("%" + (startPadding + 2) + "s", ""));

        if (page == pageMax) {
            queueMessage.append("This is the end of the queue!");
        } else {
            int moreTracks = queueSize - trackNumber;
            queueMessage.append(moreTracks).append(" more track(s)");
        }

        // Finalizes the message
        queueMessage.append("\n```");

        return queueMessage.toString();

    }

}
