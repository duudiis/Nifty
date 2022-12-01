package me.nifty.utils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    private static final Pattern specialCharacterPattern = Pattern.compile("[!@#$%¨&*_+;?|'<>.^()/\\\\\\[\\]{}-]*");

    private static final Pattern rawIntegerPattern = Pattern.compile("^([0-9]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern clockPattern = Pattern.compile("(?:([0-9]+):)?([0-9]+):([0-9]+)");
    private static final Pattern naturalTextPattern = Pattern.compile("(?:([0-9]+)(?:hours|hour|hrs|hr|h))?(?:and|:|,)?(?:([0-9]+)(?:minutes|minute|mins|min|m))?(?:and|:|,)?(?:([0-9]+)(?:seconds|secs|sec|s))?(?:and|:|,)?(?:([0-9]+)(?:milliseconds|ms|mss|mls))?$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parses a time string to a long.
     *
     * @param time The time string to parse.
     * @return The parsed time in milliseconds.
     */
    public static long parse(String time) {

        // Sanitizes the time string.
        String query = specialCharacterPattern.matcher(time).replaceAll("").replaceAll(" +", "");

        Matcher rawIntegerMatcher = rawIntegerPattern.matcher(query);

        // Checks if the time is a raw integer.
        if (rawIntegerMatcher.find() && rawIntegerMatcher.group(1) != null) {
            return Long.parseLong(rawIntegerMatcher.group(1)) * 1000;
        }

        // Checks if the time is a clock time. (HH:MM:SS:mmm)
        Matcher clockMatcher = clockPattern.matcher(query);

        if (clockMatcher.find()) {

            long hours;

            if (clockMatcher.group(1) != null) {
                hours = Long.parseLong(clockMatcher.group(1));
            } else {
                hours = 0;
            }

            long minutes = Long.parseLong(clockMatcher.group(2) != null ? clockMatcher.group(2) : "0");
            long seconds = Long.parseLong(clockMatcher.group(3) != null ? clockMatcher.group(3) : "0");

            return (hours * 3600000) + (minutes * 60000) + (seconds * 1000);

        }

        // Checks if the time is in natural format. (HHhMMm | 10 hours 30 minutes)
        Matcher naturalMatcher = naturalTextPattern.matcher(query);

        if (naturalMatcher.find()) {

            if (naturalMatcher.group(1) != null || naturalMatcher.group(2) != null || naturalMatcher.group(3) != null || naturalMatcher.group(4) != null) {

                long hours = Long.parseLong(naturalMatcher.group(1) != null ? naturalMatcher.group(1) : "0");
                long minutes = Long.parseLong(naturalMatcher.group(2) != null ? naturalMatcher.group(2) : "0");
                long seconds = Long.parseLong(naturalMatcher.group(3) != null ? naturalMatcher.group(3) : "0");
                long milliseconds = Long.parseLong(naturalMatcher.group(4) != null ? naturalMatcher.group(4) : "0");

                return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + milliseconds;

            }

        }

        return -1;

    }

}
