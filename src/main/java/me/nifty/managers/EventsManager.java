package me.nifty.managers;

import me.nifty.events.*;
import net.dv8tion.jda.api.JDABuilder;

public class EventsManager {

    /**
     * Loads the events
     * @param jdaBuilder The JDA builder
     */
    public static void load(JDABuilder jdaBuilder) {

        // Adds the events
        jdaBuilder.addEventListeners(
            new ButtonInteraction(),
            new GuildVoiceUpdate(),
            new MessageReceived(),
            new Ready(),
            new SlashCommandInteraction()
        );

    }

}
