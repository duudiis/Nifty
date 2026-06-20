package me.nifty.managers;

import me.nifty.Config;
import me.nifty.managers.interactions.AutoCompleteManager;
import me.nifty.managers.interactions.ButtonsManager;
import moe.kyokobot.libdave.NativeDaveFactory;
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class JDAManager {

    private static JDA jda;

    /**
     * Gets the built JDA instance.
     * @return The JDA instance, or null if it has not been built yet.
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * Creates the JDA instance
     */
    public static void create() {

        System.out.println("[Nifty] Creating JDA instance...");

        // Creates the JDA instance
        JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getDiscordToken())
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new LDJDADaveSessionFactory(new NativeDaveFactory())));

        // Sets the activity
        jdaBuilder.setActivity(Activity.listening("/play"));

        // Loads the settings into the JDA instance
        setSettings(jdaBuilder);

        // Loads the commands
        CommandsManager.load();

        // Listen for events
        EventsManager.load(jdaBuilder);

        // Loads the audio player manager
        AudioManager.load();

        // Loads the buttons
        ButtonsManager.load();

        // Loads the auto completes
        AutoCompleteManager.load();

        // Builds the JDA instance
        jda = jdaBuilder.build();

        // Connects the optional dashboard add-on (no-op if DASHBOARD_WS_URL is unset)
        WebSocketManager.connect();

    }

    /**
     * Sets the settings for the JDA instance
     * @param jdaBuilder The JDA builder
     */
    public static void setSettings(JDABuilder jdaBuilder) {

        jdaBuilder.setMemberCachePolicy(
                MemberCachePolicy.VOICE
        );

        jdaBuilder.disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.SCHEDULED_EVENTS,
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.FORUM_TAGS
        );

        jdaBuilder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES
        );

        jdaBuilder.disableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.DIRECT_MESSAGE_TYPING,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_WEBHOOKS
        );

    }

}
