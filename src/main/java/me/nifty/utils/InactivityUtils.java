package me.nifty.utils;

import me.nifty.core.database.guild.GuildHandler;
import me.nifty.utils.enums.InactivityType;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class InactivityUtils {

    private static final Map<InactivityType, Long> inactivityTimes = Map.of(
            InactivityType.PAUSED, 60 * 60 * 1000L, // Paused for 1 hour
            InactivityType.STOPPED, 15 * 60 * 1000L, // Stopped for 15 minutes
            InactivityType.ALONE, 5 * 60 * 1000L // Alone for 5 minutes
    );

    private static final Map<InactivityType, Map<Long, Timer>> activeTimers = Map.of(
            InactivityType.PAUSED, new HashMap<>(),
            InactivityType.STOPPED, new HashMap<>(),
            InactivityType.ALONE, new HashMap<>()
    );

    /**
     * Starts the inactivity timer for the specified type for the specified guild
     *
     * @param type The type of inactivity
     * @param guild The guild to start the timer for
     */
    public static void startTimer(InactivityType type, Guild guild) {

        // Stops the current timer, if any
        stopTimer(type, guild);

        Boolean is247 = GuildHandler.getInactivityDisconnect(guild.getIdLong());
        if (is247) { return; }

        // Creates a new timer
        Timer timer = new Timer();

        // Schedules the timer to run the specified task after the specific delay for the specific type
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                VoiceUtils.inactivityDisconnect(guild);
            }
        }, inactivityTimes.get(type));

        // Adds the timer to the active timers map
        activeTimers.get(type).put(guild.getIdLong(), timer);

    }

    /**
     * Stops the inactivity timer for the specified type for the specified guild
     *
     * @param type The type of inactivity
     * @param guild The guild to stop the timer for
     */
    public static void stopTimer(InactivityType type, Guild guild) {

        // If the guild has an active timer for the specified type
        if (activeTimers.get(type).containsKey(guild.getIdLong())) {
            // Cancels the timer
            activeTimers.get(type).get(guild.getIdLong()).cancel();

            // Removes the timer from the active timers map
            activeTimers.get(type).remove(guild.getIdLong());
        }

    }

}
