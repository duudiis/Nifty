package me.nifty.utils.formatting;

public class TrackTime {

    /**
     * Formats the time of a track to a clock format.
     *
     * @param time The time to format.
     * @return The formatted time in HH:MM:SS.
     */
    public static String formatClock(long time) {

        // Gets the hours, minutes and seconds of the time

        long hours = time / 3600000;
        long minutes = (time % 3600000) / 60000;
        long seconds = (time % 60000) / 1000;

        if (hours > 0) {

            // If the time is longer than an hour, formats the time to hours, minutes and seconds
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);

        } else {

            // If the time is shorter than an hour, formats the time to minutes and seconds
            return String.format("%02d:%02d", minutes, seconds);

        }


    }

    /**
     * Formats the time of a track to a natural text format.
     *
     * @param time The time to format.
     * @return The formatted time in natural text.
     */
    public static String formatNatural(long time) {

        // Gets the hours, minutes and seconds of the time

        long hours = time / 3600000;
        long minutes = (time % 3600000) / 60000;
        long seconds = (time % 60000) / 1000;

        if (hours > 0) {

            // If the time is longer than an hour, formats the time to hours, minutes and seconds
            return String.format("%dh %dm %ds", hours, minutes, seconds);

        } else if (minutes > 0) {

            // If the time is longer than a minute, formats the time to minutes and seconds
            return String.format("%dm %ds", minutes, seconds);

        } else {

            // If the time is shorter than a minute, formats the time to seconds
            return String.format("%ds", seconds);

        }

    }

    /**
     * Formats the time of a track to a percentage bar format.
     *
     * @param position The time to format.
     * @param duration The total time of the track.
     * @return The formatted time in a bar.
     */
    public static String formatBar(long position, long duration) {

        // Gets the length of the bar
        int barLength = 20;
        StringBuilder durationBar = new StringBuilder();

        int durationBarProgress = (int) (position * barLength / duration);

        for (int i = 0; i < barLength; i++) {

            if (i == durationBarProgress) {
                durationBar.append("\uD83D\uDD35");
            } else {
                durationBar.append("\u25AC");
            }

        }

        return durationBar.toString();

    }

}
