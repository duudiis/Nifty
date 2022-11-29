package me.nifty.utils.parser;

import java.util.List;
import java.util.Map;

public class BoolParser {

    private static final Map<Boolean, List<String>> boolKeywords = Map.of(
            (true), List.of("e", "enable", "enabled", "on", "yes", "true"),
            (false), List.of("d", "disable", "disabled", "off", "no", "false")
    );

    public static boolean parse(String query, boolean currentBool) {

        for (boolean bool : boolKeywords.keySet()) {
            if (boolKeywords.get(bool).contains(query.toLowerCase())) {
                return bool;
            }
        }

        return !currentBool;

    }

}
