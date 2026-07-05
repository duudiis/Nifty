package me.nifty.websocket;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.JDAManager;
import me.nifty.utils.VoiceUtils;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;
import me.nifty.websocket.payloads.WsDelta;
import me.nifty.websocket.payloads.WsSessions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes player-control actions requested by the dashboard. Pure delegation
 * into the same managers the Discord commands use — the dashboard is just
 * another caller, never a special code path.
 */
public class DashboardActions {

    static void handle(JSONObject data) {

        long guildId = data.optLong("guildId", 0);
        String action = data.optString("action", "");

        // "summon" has no player yet — it creates one by joining the user's VC.
        if (action.equals("summon")) {
            summon(guildId, data.optLong("userId", 0));
            return;
        }

        PlayerManager playerManager = PlayerManager.get(guildId);

        // Queuing while not connected: join the requester's voice channel first
        // (exactly like the play command does), then enqueue.
        if (action.equals("play")) {
            long userId = data.optLong("userId", 0);
            if (playerManager == null) {
                playerManager = connectToUser(guildId, userId);
            }
            if (playerManager != null) {
                // Translate the dashboard's intent into loader flags:
                //   now  -> insert right after current AND jump to it (play instantly)
                //   next -> insert right after current
                List<String> flags = new ArrayList<>();
                if (data.optBoolean("now", false)) {
                    flags.add("next");
                    flags.add("jump");
                } else if (data.optBoolean("next", false)) {
                    flags.add("next");
                }
                enqueue(playerManager, userId, data.optString("query", ""), flags);
            }
            return;
        }

        // Other actions only make sense for a guild this bot is already playing in.
        if (playerManager == null) { return; }

        switch (action) {

            case "togglePause" -> {
                playerManager.getAudioPlayer().setPaused(!playerManager.getAudioPlayer().isPaused());
                WsDelta.player(playerManager);
            }

            case "back" -> {
                playerManager.getTrackScheduler().back();
                unpause(playerManager);
            }

            case "skip" -> {
                playerManager.getTrackScheduler().skip();
                unpause(playerManager);
            }

            case "jump" -> {
                int position = resolvePosition(playerManager, data);
                if (position >= 0) {
                    playerManager.getTrackScheduler().jump(position);
                    unpause(playerManager);
                }
            }

            // Queued "Play now": move the entry right after the current track and
            // jump to it (keeps the rest of the queue in order), then unpause.
            case "playNow" -> {
                int position = resolvePosition(playerManager, data);
                if (position >= 0) {
                    playerManager.getTrackScheduler().playNow(position);
                    unpause(playerManager);
                }
            }

            // Queued "Play next": move the entry right after the current track.
            case "playNext" -> {
                int position = resolvePosition(playerManager, data);
                if (position >= 0) {
                    playerManager.getTrackScheduler().moveAfterCurrent(position);
                }
            }

            // Queued "Move to last": move the entry to the end of the queue.
            case "moveToLast" -> {
                int position = resolvePosition(playerManager, data);
                if (position >= 0) {
                    playerManager.getTrackScheduler().moveToLast(position);
                }
            }

            // Drag-reorder: move a queue entry to an explicit index.
            case "move" -> {
                int position = resolvePosition(playerManager, data);
                int toIndex = data.optInt("toIndex", -1);
                if (position >= 0 && toIndex >= 0) {
                    playerManager.getTrackScheduler().move(position, toIndex);
                }
            }

            case "loop" -> {
                Loop current = playerManager.getPlayerHandler().getLoopMode();
                Loop next = switch (current) {
                    case DISABLED -> Loop.QUEUE;
                    case QUEUE -> Loop.TRACK;
                    default -> Loop.DISABLED;
                };
                playerManager.getPlayerHandler().setLoopMode(next);
                WsDelta.player(playerManager);
            }

            case "shuffle" -> {
                Shuffle current = playerManager.getPlayerHandler().getShuffleMode();
                if (current == Shuffle.DISABLED) {
                    playerManager.getPlayerHandler().setShuffleMode(Shuffle.ENABLED);
                    int position = playerManager.getPlayerHandler().getPosition();
                    // shuffleAfter emits its own q_resync — no queue nudge here.
                    playerManager.getQueueHandler().shuffleAfter(position + 1);
                } else {
                    playerManager.getPlayerHandler().setShuffleMode(Shuffle.DISABLED);
                }
                WsDelta.player(playerManager);
            }

            case "volume" -> {
                int volume = data.optInt("volume", 100);
                playerManager.getAudioPlayer().setVolume(volume);
                playerManager.getPlayerHandler().setVolume(volume);
                WsDelta.player(playerManager);
            }

            case "seek" -> {
                AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();
                if (playingTrack != null) {
                    long position = data.optLong("position", 0);
                    playingTrack.setPosition(position);
                    // Re-anchor the wall-clock playback position after the seek
                    playerManager.getPlayerHandler().anchorPosition(position);
                    WsDelta.player(playerManager);
                }
            }

            case "remove" -> {
                int position = resolvePosition(playerManager, data);
                if (position >= 0) {
                    playerManager.getTrackScheduler().remove(position);
                }
            }

            case "clear" -> playerManager.getTrackScheduler().clear();

            default -> { /* unknown action — ignore */ }

        }

    }

    /**
     * Resolves the queue entry an action addresses. Preferred: the stable
     * entryId (the queue_tracks row id), immune to concurrent queue shifts —
     * a stale one resolves to -1 and the action safely no-ops instead of
     * hitting whatever track slid into the old position. Falls back to the
     * raw trackId (position) for older clients.
     */
    private static int resolvePosition(PlayerManager playerManager, JSONObject data) {

        long entryId = data.optLong("entryId", -1);
        if (entryId != -1) {
            return playerManager.getQueueHandler().getEntryPosition(entryId);
        }

        return data.optInt("trackId", -1);

    }

    private static void enqueue(PlayerManager playerManager, long userId, String query, List<String> flags) {

        if (query == null || query.isBlank() || userId == 0) { return; }

        Guild guild = playerManager.getGuild();
        if (guild == null) { return; }

        Member member = guild.getMemberById(userId);
        if (member == null) { return; }

        // Reply target is null: the dashboard, not a Discord channel, requested this.
        playerManager.getTrackScheduler().queue(query, null, member, flags == null ? new ArrayList<>() : flags);

    }

    /**
     * Resumes playback if the player is paused, then pushes fresh player state.
     * Used after dashboard-initiated track changes (jump/back/skip/play-now) so a
     * paused bot starts playing the new track rather than landing on it paused.
     */
    private static void unpause(PlayerManager playerManager) {
        if (playerManager.getAudioPlayer().isPaused()) {
            playerManager.getAudioPlayer().setPaused(false);
            WsDelta.player(playerManager);
        }
    }

    /**
     * Joins the requesting user's voice channel in the given guild and returns
     * the (now-created) player. No-op if the bot is already connected there, the
     * user isn't in a voice channel, or anything is missing.
     *
     * @return the guild's PlayerManager once connected, or {@code null}.
     */
    private static PlayerManager connectToUser(long guildId, long userId) {

        if (guildId == 0 || userId == 0) { return null; }

        try {

            // Already connected somewhere in this guild — don't move it.
            PlayerManager existing = PlayerManager.get(guildId);
            if (existing != null) { return existing; }

            JDA jda = JDAManager.getJDA();
            if (jda == null) { return null; }

            Guild guild = jda.getGuildById(guildId);
            if (guild == null) { return null; }

            Member member = guild.getMemberById(userId);
            if (member == null) { return null; }

            GuildVoiceState voiceState = member.getVoiceState();
            if (voiceState == null || !voiceState.inAudioChannel()) { return null; }

            AudioChannel channel = voiceState.getChannel();
            if (!(channel instanceof VoiceChannel voiceChannel)) { return null; }

            VoiceUtils.join(voiceChannel);
            return PlayerManager.get(guildId);

        } catch (Exception ignored) {
            // Connecting must never break the bot.
            return null;
        }

    }

    /**
     * Joins the requesting user's voice channel in the given (dashboard-selected)
     * guild, then pushes fresh state so the dashboard reflects the new session.
     */
    private static void summon(long guildId, long userId) {

        PlayerManager playerManager = connectToUser(guildId, userId);
        if (playerManager != null) {
            WsDelta.player(playerManager);
            WsDelta.qResync(guildId);
        }
        WsSessions.reply(userId);

    }

}
