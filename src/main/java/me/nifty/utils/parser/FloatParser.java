package me.nifty.utils.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatParser {

    private static final Pattern floatPattern = Pattern.compile("([0-9]*[.])?[0-9]+");

    private static final List<String> resetKeywords = List.of("reset", "r", "default", "d", "off", "disable", "disabled", "no", "false", "stop");

    public static Float parse(String input) {

        if (resetKeywords.contains(input.toLowerCase())) {
            return -1f;
        }

        Matcher floatMatcher = floatPattern.matcher(input);

        if (floatMatcher.find()) {
            return Float.parseFloat(floatMatcher.group(0));
        }

        return null;

    }

}
