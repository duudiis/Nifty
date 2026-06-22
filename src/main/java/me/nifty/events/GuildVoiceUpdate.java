package me.nifty.events;

import me.nifty.core.music.PlayerManager;
import me.nifty.utils.InactivityUtils;
import me.nifty.utils.VoiceUtils;
import me.nifty.utils.enums.InactivityType;
import me.nifty.utils.formatting.WsSessions;
import me.nifty.websocket.WebSocketClientEndpoint;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashSet;
import java.util.Set;

public class GuildVoiceUpdate extends ListenerAdapter {

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {

        Member updatedMember = event.getEntity();
        boolean isSelf = updatedMember.getIdLong() == event.getGuild().getSelfMember().getIdLong();

        if (isSelf) {
            selfVoiceUpdate(event);
        }

        // Keep open dashboards' Connect list current: a voice change alters who
        // can control which session. Push fresh sessions to the affected user(s).
        pushDashboardSessions(event, updatedMember, isSelf);

        if (updatedMember.getUser().isBot()) { return; }

        memberVoiceUpdate(event);

    }

    /** Pushes refreshed sessions to the dashboard for whoever's view just changed. */
    private void pushDashboardSessions(GuildVoiceUpdateEvent event, Member updatedMember, boolean isSelf) {

        if (!WebSocketClientEndpoint.isConnected()) { return; }

        try {
            if (isSelf) {
                // The bot moved: everyone in the channels it joined/left is affected.
                Set<Long> userIds = new HashSet<>();
                collectMembers(event.getChannelJoined(), userIds);
                collectMembers(event.getChannelLeft(), userIds);
                for (long userId : userIds) {
                    WsSessions.reply(userId);
                }
            } else if (!updatedMember.getUser().isBot()) {
                WsSessions.reply(updatedMember.getIdLong());
            }
        } catch (Exception ignored) {
            // Dashboard sync must never break voice handling.
        }

    }

    private void collectMembers(AudioChannel channel, Set<Long> userIds) {
        if (channel == null) { return; }
        for (Member member : channel.getMembers()) {
            if (!member.getUser().isBot()) {
                userIds.add(member.getIdLong());
            }
        }
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

            // Gets the number of members (not bots) in the voice channel
            int membersInChannel = event.getChannelJoined().getMembers().stream().filter(member -> !member.getUser().isBot()).toList().size();

            // If there are no members in the voice channel, starts an inactivity timer
            if (membersInChannel == 0) {
                InactivityUtils.startTimer(InactivityType.ALONE, event.getGuild());
            } else {
                // If there are members in the voice channel, cancels the inactivity timer
                InactivityUtils.stopTimer(InactivityType.ALONE, event.getGuild());
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

        AudioManager JDAAudioManager = event.getGuild().getAudioManager();
        if (!JDAAudioManager.isConnected() || JDAAudioManager.getConnectedChannel() == null) { return; }

        VoiceChannel voiceChannel = JDAAudioManager.getConnectedChannel().asVoiceChannel();

        // If the member joined / moved to a voice channel and the bot is in the same voice channel
        if (event.getChannelJoined() != null && event.getChannelJoined().getIdLong() == voiceChannel.getIdLong()) {

            // Gets the number of members (not bots) in the voice channel
            int membersInChannel = event.getChannelJoined().getMembers().stream().filter(member -> !member.getUser().isBot()).toList().size();

            // If there are no members in the voice channel, starts an inactivity timer
            if (membersInChannel == 0) {
                InactivityUtils.startTimer(InactivityType.ALONE, event.getGuild());
            } else {
                // If there are members in the voice channel, cancels the inactivity timer
                InactivityUtils.stopTimer(InactivityType.ALONE, event.getGuild());
            }

        }

        // If the member left a voice channel and the bot was in the same voice channel
        if (event.getChannelLeft() != null && event.getChannelLeft().getIdLong() == voiceChannel.getIdLong()) {

            // Gets the number of members (not bots) in the voice channel
            int membersInChannel = event.getChannelLeft().getMembers().stream().filter(member -> !member.getUser().isBot()).toList().size();

            // If there are no members in the voice channel, starts an inactivity timer
            if (membersInChannel == 0) {
                InactivityUtils.startTimer(InactivityType.ALONE, event.getGuild());
            } else {
                // If there are members in the voice channel, cancels the inactivity timer
                InactivityUtils.stopTimer(InactivityType.ALONE, event.getGuild());
            }

        }

    }

}
