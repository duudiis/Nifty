package me.nifty.websocket;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.Config;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.JDAManager;
import me.nifty.utils.VoiceUtils;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;
import me.nifty.utils.formatting.WsPlayer;
import me.nifty.utils.formatting.WsQueue;
import me.nifty.utils.formatting.WsSessions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Optional dashboard add-on: a resilient WebSocket client that connects the bot
 * to the Nifty Dashboard hub. It is intentionally fire-and-forget — every send
 * is guarded, every inbound handler is wrapped, and a dead/slow/broken dashboard
 * can never throw into the bot's audio paths. When the dashboard is unreachable
 * the client simply keeps trying to reconnect in the background while the bot
 * continues to operate normally.
 */
public class WebSocketClientEndpoint {

    private static volatile WebSocketClient wsClient;
    private static volatile Timer heartbeatTimer;
    private static volatile boolean shuttingDown = false;

    private static final int RECONNECT_DELAY_MS = 10_000;

    public WebSocketClientEndpoint(URI serverURI) {

        System.out.println("[Nifty] Dashboard WebSocket initialising -> " + serverURI);

        wsClient = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("[Nifty] Dashboard WebSocket connected.");
                identify();
            }

            @Override
            public void onMessage(String message) {
                try {
                    handleMessage(new JSONObject(message));
                } catch (Exception e) {
                    // A malformed/unexpected dashboard message must never crash the bot.
                    System.out.println("[Nifty] Ignored a malformed dashboard message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                stopHeartbeat();
                if (shuttingDown) { return; }
                System.out.println("[Nifty] Dashboard WebSocket closed (" + code + "). Reconnecting in " + (RECONNECT_DELAY_MS / 1000) + "s...");
                scheduleReconnect();
            }

            @Override
            public void onError(Exception ex) {
                // Swallow: the dashboard being down is not a bot error.
            }

        };

        // Never block the boot thread; connection failures are handled by onClose.
        try {
            wsClient.connect();
        } catch (Exception e) {
            scheduleReconnect();
        }

    }

    /* -------------------------------------------------------------------- */
    /*  Outbound                                                            */
    /* -------------------------------------------------------------------- */

    /**
     * Sends a message to the dashboard if (and only if) the socket is open.
     * Always safe to call — does nothing when the dashboard is unavailable.
     *
     * @param message The raw message to send.
     */
    public static void send(String message) {
        try {
            WebSocketClient client = wsClient;
            if (client != null && client.isOpen()) {
                client.send(message);
            }
        } catch (Exception ignored) {
            // Never propagate dashboard transport errors.
        }
    }

    /**
     * @return {@code true} if the dashboard socket is currently connected.
     */
    public static boolean isConnected() {
        WebSocketClient client = wsClient;
        return client != null && client.isOpen();
    }

    private void identify() {
        JSONObject identify = new JSONObject();
        identify.put("operation", "identify");

        JSONObject data = new JSONObject();
        data.put("token", Config.getDashboardToken());
        data.put("botName", Config.getDashboardBotName());

        identify.put("data", data);

        send(identify.toString());
    }

    /* -------------------------------------------------------------------- */
    /*  Inbound                                                             */
    /* -------------------------------------------------------------------- */

    private void handleMessage(JSONObject json) {

        String operation = json.optString("operation", "");
        JSONObject data = json.optJSONObject("data");
        if (data == null) { data = new JSONObject(); }

        switch (operation) {

            case "hello" -> startHeartbeat(data.optInt("heartbeatInterval", 45_000));

            case "heartbeat_ack" -> { /* keep-alive acknowledged */ }

            case "identify_success" -> System.out.println("[Nifty] Dashboard authenticated this bot.");

            case "identify_error" -> System.out.println("[Nifty] Dashboard rejected this bot's token.");

            // The dashboard asks every connected bot which sessions a user can control.
            case "sessions_request" -> WsSessions.reply(data.optLong("userId", 0));

            // A user selected a guild: push the current player + queue for it.
            case "subscribe" -> {
                long guildId = data.optLong("guildId", 0);
                PlayerManager playerManager = PlayerManager.get(guildId);
                if (playerManager != null) {
                    WsPlayer.updateWsPlayer(playerManager);
                    WsQueue.updateWsQueue(guildId);
                }
            }

            case "action" -> handleAction(data);

            default -> { /* unknown operation — ignore */ }

        }

    }

    private void handleAction(JSONObject data) {

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
                WsPlayer.updateWsPlayer(playerManager);
            }

            case "back" -> playerManager.getTrackScheduler().back();

            case "skip" -> playerManager.getTrackScheduler().skip();

            case "jump" -> playerManager.getTrackScheduler().jump(data.optInt("trackId", 0));

            // Move a queue entry right after the current track (so it plays next).
            case "playNext" -> {
                int cur = playerManager.getPlayerHandler().getPosition();
                int trackId = data.optInt("trackId", -1);
                if (trackId >= 0 && trackId != cur) {
                    playerManager.getTrackScheduler().move(trackId, cur + 1);
                }
            }

            // Move a queue entry to the very first position.
            case "moveToTop" -> {
                int trackId = data.optInt("trackId", -1);
                if (trackId >= 0) {
                    playerManager.getTrackScheduler().move(trackId, 0);
                }
            }

            // Move a queue entry to the very last position.
            case "moveToLast" -> {
                int trackId = data.optInt("trackId", -1);
                int last = playerManager.getQueueHandler().getQueueSize() - 1;
                if (trackId >= 0 && last >= 0) {
                    playerManager.getTrackScheduler().move(trackId, last);
                }
            }

            // Drag-reorder: move a queue entry to an explicit index.
            case "move" -> {
                int trackId = data.optInt("trackId", -1);
                int toIndex = data.optInt("toIndex", -1);
                if (trackId >= 0 && toIndex >= 0) {
                    playerManager.getTrackScheduler().move(trackId, toIndex);
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
                WsPlayer.updateWsPlayer(playerManager);
            }

            case "shuffle" -> {
                Shuffle current = playerManager.getPlayerHandler().getShuffleMode();
                if (current == Shuffle.DISABLED) {
                    playerManager.getPlayerHandler().setShuffleMode(Shuffle.ENABLED);
                    int position = playerManager.getPlayerHandler().getPosition();
                    playerManager.getQueueHandler().shuffleAfter(position + 1);
                    WsQueue.updateWsQueue(guildId);
                } else {
                    playerManager.getPlayerHandler().setShuffleMode(Shuffle.DISABLED);
                }
                WsPlayer.updateWsPlayer(playerManager);
            }

            case "volume" -> {
                playerManager.getAudioPlayer().setVolume(data.optInt("volume", 100));
                WsPlayer.updateWsPlayer(playerManager);
            }

            case "seek" -> {
                AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();
                if (playingTrack != null) {
                    playingTrack.setPosition(data.optLong("position", 0));
                    WsPlayer.updateWsPlayer(playerManager);
                }
            }

            case "remove" -> playerManager.getTrackScheduler().remove(data.optInt("trackId", 0));

            case "clear" -> playerManager.getTrackScheduler().clear();

            default -> { /* unknown action — ignore */ }

        }

    }

    private void enqueue(PlayerManager playerManager, long userId, String query, List<String> flags) {

        if (query == null || query.isBlank() || userId == 0) { return; }

        Guild guild = playerManager.getGuild();
        if (guild == null) { return; }

        Member member = guild.getMemberById(userId);
        if (member == null) { return; }

        // Reply target is null: the dashboard, not a Discord channel, requested this.
        playerManager.getTrackScheduler().queue(query, null, member, flags == null ? new ArrayList<>() : flags);

    }

    /**
     * Joins the requesting user's voice channel in the given guild and returns
     * the (now-created) player. No-op if the bot is already connected there, the
     * user isn't in a voice channel, or anything is missing.
     *
     * @return the guild's PlayerManager once connected, or {@code null}.
     */
    private PlayerManager connectToUser(long guildId, long userId) {

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
    private void summon(long guildId, long userId) {

        PlayerManager playerManager = connectToUser(guildId, userId);
        if (playerManager != null) {
            WsPlayer.updateWsPlayer(playerManager);
            WsQueue.updateWsQueue(guildId);
        }
        WsSessions.reply(userId);

    }

    /* -------------------------------------------------------------------- */
    /*  Heartbeat & reconnect                                              */
    /* -------------------------------------------------------------------- */

    private void startHeartbeat(int interval) {

        stopHeartbeat();

        Timer timer = new Timer("dashboard-heartbeat", true);
        heartbeatTimer = timer;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("operation", "heartbeat");
                send(heartbeat.toString());
            }
        }, 0, Math.max(1_000, interval));

    }

    private static void stopHeartbeat() {
        Timer timer = heartbeatTimer;
        if (timer != null) {
            timer.cancel();
            timer.purge();
            heartbeatTimer = null;
        }
    }

    private void scheduleReconnect() {
        new Timer("dashboard-reconnect", true).schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    WebSocketClient client = wsClient;
                    if (client != null && !shuttingDown) {
                        client.reconnect();
                    }
                } catch (Exception ignored) { }
            }
        }, RECONNECT_DELAY_MS);
    }

    /**
     * Cleanly stops the dashboard client (used on shutdown). Optional.
     */
    public static void shutdown() {
        shuttingDown = true;
        stopHeartbeat();
        try {
            WebSocketClient client = wsClient;
            if (client != null) { client.close(); }
        } catch (Exception ignored) { }
    }

}
