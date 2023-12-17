package me.nifty.managers;

import me.nifty.Config;
import me.nifty.managers.interactions.AutoCompleteManager;
import me.nifty.managers.interactions.ButtonsManager;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class JDAManager {

    /**
     * Creates the JDA instance
     */
    public static void create() {

        System.out.println("[Nifty] Creating JDA instance...");

        // Creates the JDA instance
        JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getDiscordToken());

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
        jdaBuilder.build();

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
                GatewayIntent.GUILD_BANS,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS
        );

    }

}
