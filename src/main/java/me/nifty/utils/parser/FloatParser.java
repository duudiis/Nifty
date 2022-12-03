package me.nifty.utils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatParser {

    private static final Pattern floatPattern = Pattern.compile("([0-9]*[.])?[0-9]+");

    public static Float parse(String input) {

        Matcher floatMatcher = floatPattern.matcher(input);

        if (floatMatcher.find()) {
            return Float.parseFloat(floatMatcher.group(0));
        }

        return null;

    }

}
