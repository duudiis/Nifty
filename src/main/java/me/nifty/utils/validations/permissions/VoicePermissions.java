package me.nifty.utils.validations.permissions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class VoicePermissions {

    /**
     * Checks if the member has all permissions to connect to a voice channel
     * @param voiceChannel The voice channel to check the permissions
     * @return The error message if any, null if no error.
     */
    public static String validate(VoiceChannel voiceChannel) {

        // Gets the bot member
        Member selfMember = voiceChannel.getGuild().getSelfMember();

        // Validate permissions to view channel
        if (!selfMember.hasPermission(voiceChannel, Permission.VIEW_CHANNEL)) {
            return "I do not have permission to **view** your voice channel!";
        }

        // Validate permissions to connect to channel
        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            return "I do not have permission to **connect** to your voice channel!";
        }

        // Validate permissions to speak in channel
        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_SPEAK)) {
            return "I do not have permission to **speak** in your voice channel!";
        }

        // Validate permissions to move members, if the channel is full
        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS) && voiceChannel.getMembers().size() >= voiceChannel.getUserLimit() && voiceChannel.getUserLimit() != 0) {
            return "I do not have permission to **connect** to your voice channel! (Full)";
        }

        return null;

    }

}
