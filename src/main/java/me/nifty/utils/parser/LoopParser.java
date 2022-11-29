package me.nifty.utils.parser;

import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Loop;

import java.util.List;
import java.util.Map;

public class LoopParser {

    private static final Map<Loop, List<String>> loopKeywords = Map.of(
            Loop.DISABLED, List.of("d", "disable", "disabled", "off", "no", "false", "stop"),
            Loop.QUEUE, List.of("q", "queue", "all"),
            Loop.TRACK, List.of("t", "track", "song", "c", "current", "t", "this", "n", "now", "np", "playing")
    );

    public static Loop parse(String query, PlayerManager playerManager) {

        for (Loop loop : Loop.values()) {
            if (loopKeywords.get(loop).contains(query.toLowerCase())) {
                return loop;
            }
        }

        Loop currentLoop = playerManager.getPlayerHandler().getLoopMode();

        if (currentLoop == Loop.DISABLED) {
            return Loop.QUEUE;
        } else if (currentLoop == Loop.QUEUE) {
            return Loop.TRACK;
        } else {
            return Loop.DISABLED;
        }

    }

}
