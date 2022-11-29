package me.nifty.utils.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {

    private final List<String> flags;
    private final String query;

    private static final List<String> defaultFlags = List.of("all", "choose", "jump", "next", "reverse", "shuffle");
    private static final Pattern URLPattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)", Pattern.CASE_INSENSITIVE);

    public QueryParser(List<String> flags, String query) {
        this.flags = flags;
        this.query = query;
    }

    /**
     * Gets the flags from the query.
     * @return The flags
     */
    public List<String> getFlags() {
        return flags;
    }

    /**
     * Gets the query string.
     * @return The query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Parses a query string into a QueryParser object.
     * @param query The query string to parse.
     * @return A QueryParser object containing the parsed query.
     */
    public static QueryParser parse(String query) {

        // Creates a map for shortened flags.
        Map<String, String> shortenedFlags = new HashMap<>();

        // Adds shortened flags to the map.
        for (String flag : defaultFlags) {
            shortenedFlags.put(flag.substring(0, 1), flag);
        }

        // Creates a list for the flags.
        List<String> foundFlags = new ArrayList<>();

        // Splits the query into an array of words.
        String[] words = query.split(" ");

        // Loops through the words.
        for (String word : words) {

            // Loops through the shortened flags.
            for (String flag : shortenedFlags.keySet()) {

                // Checks if the word is a shortened flag.
                if (word.equals("-" + flag)) {

                    // Removes the flag from the query.
                    query = query.replace("-" + flag, "");

                    // Adds the flag to the list.
                    foundFlags.add(shortenedFlags.get(flag));

                }

            }

            // Loops through the default flags.
            for (String flag : defaultFlags) {

                // Checks if the word is a default flag.
                if (word.equals("-" + flag)) {

                    // Removes the flag from the query.
                    query = query.replace("-" + flag, "");

                    // Adds the flag to the list.
                    foundFlags.add(flag);

                }
            }

        }

        // Checks if the query contains a URL.
        Matcher matcher = URLPattern.matcher(query);

        // If the query contains a URL, returns it.
        if (matcher.find()) {
            String url = matcher.group(0).replace("&list=LL", "");
            return new QueryParser(foundFlags, url);
        }

        // Return the query as a YouTube search query.
        return new QueryParser(foundFlags, "ytsearch:" + query);

    }

}
