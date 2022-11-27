package me.nifty.events;

import me.nifty.core.music.PlayerManager;
import me.nifty.utils.VoiceUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildVoiceUpdate extends ListenerAdapter {

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {

        Member updatedMember = event.getEntity();

        if (updatedMember.getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
            selfVoiceUpdate(event);
        }

        if (updatedMember.getUser().isBot()) { return; }

        memberVoiceUpdate(event);

    }

    /**
     * Handles the event when the bot is the one who updated
     * @param event The event
     */
    private void selfVoiceUpdate(GuildVoiceUpdateEvent event) {

        // If the bot joined / moved to a voice channel
        if (event.getChannelJoined() != null) {

            PlayerManager playerManager = PlayerManager.get(event.getGuild());

            // Updates the voice channel id on the database
            if (playerManager != null) {
                playerManager.getPlayerHandler().setVoiceChannelId(event.getChannelJoined().getIdLong());
            }

            // Server deafens the bot, if it has the permission
            if (event.getEntity().hasPermission(event.getChannelJoined(), Permission.VOICE_DEAF_OTHERS)) {
                event.getEntity().deafen(true).queue();
            }

        }

        // If the bot left a voice channel, clears the player
        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            VoiceUtils.disconnect(event.getGuild());
        }

    }

    /**
     * Handles the event when a member is the one who updated
     * @param event The event
     */
    private void memberVoiceUpdate(GuildVoiceUpdateEvent event) {

    }

}
