package me.nifty.utils.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RangeParser {

    private static final Pattern rangePattern = Pattern.compile("([0-9]+)(?: *(?:-|to|till|until) *| +)([0-9]+)", Pattern.CASE_INSENSITIVE);

    public static int[] parse(String query) {

        Matcher rangeMatcher = rangePattern.matcher(query);

        if (rangeMatcher.find()) {

            int start = Integer.parseInt(rangeMatcher.group(1)) - 1;
            int end = Integer.parseInt(rangeMatcher.group(2)) - 1;

            return new int[]{start, end};

        }

        return new int[]{-1};

    }

}
