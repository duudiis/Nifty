package me.nifty.events;

import me.nifty.core.analytics.PlaybackAnalytics;
import me.nifty.core.database.UserStore;
import me.nifty.core.music.PlayerManager;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Feeds mute/deafen changes into the listening analytics:
 *
 * <ul>
 *   <li>The bot getting server-muted silences everyone → all segments close
 *       with {@code bot_mute} and reopen on {@code bot_unmute}.</li>
 *   <li>A user deafening (self or server) stops only their own hearing →
 *       their segment closes with {@code user_deafen} and reopens on
 *       {@code user_undeafen}.</li>
 * </ul>
 */
public class GuildVoiceMuteDeafen extends ListenerAdapter {

    @Override
    public void onGuildVoiceMute(GuildVoiceMuteEvent event) {

        // Only the bot's own mute state silences the whole channel.
        if (event.getMember().getIdLong() != event.getGuild().getSelfMember().getIdLong()) { return; }

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        if (playerManager == null) { return; }

        try {

            if (event.isMuted()) {
                playerManager.getPlaybackAnalytics().botMuted();
            } else if (playerManager.getAudioPlayer().getPlayingTrack() != null
                    && !playerManager.getAudioPlayer().isPaused()) {
                playerManager.getPlaybackAnalytics().botUnmuted(
                        PlaybackAnalytics.snapshotListeners(event.getGuild()));
            }

        } catch (Exception ignored) {
            // Analytics must never break voice handling.
        }

    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {

        Member member = event.getMember();

        // The bot server-deafens itself on join — its own deafen state never
        // affects what users hear.
        if (member.getUser().isBot()) { return; }

        PlayerManager playerManager = PlayerManager.get(event.getGuild());
        if (playerManager == null) { return; }

        try {

            // Only members in the bot's channel are listeners.
            AudioChannel botChannel = event.getGuild().getAudioManager().getConnectedChannel();
            GuildVoiceState voiceState = member.getVoiceState();

            if (botChannel == null || voiceState == null || voiceState.getChannel() == null
                    || voiceState.getChannel().getIdLong() != botChannel.getIdLong()) {
                return;
            }

            boolean deafened = voiceState.isDeafened() || voiceState.isSelfDeafened();

            if (deafened) {
                playerManager.getPlaybackAnalytics().userStoppedListening(member.getIdLong(), "user_deafen");
            } else if (playerManager.getAudioPlayer().getPlayingTrack() != null
                    && !playerManager.getAudioPlayer().isPaused()
                    && PlaybackAnalytics.isBotAudible(event.getGuild())) {
                playerManager.getPlaybackAnalytics().userStartedListening(
                        UserStore.UserRef.of(member), "user_undeafen");
            }

        } catch (Exception ignored) {
            // Analytics must never break voice handling.
        }

    }

}
