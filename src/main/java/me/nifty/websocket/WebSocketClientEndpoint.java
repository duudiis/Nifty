package me.nifty.websocket;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.utils.enums.Loop;
import me.nifty.utils.enums.Shuffle;
import me.nifty.utils.formatting.WsGuild;
import me.nifty.utils.formatting.WsPlayer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketClientEndpoint {

    static WebSocketClient wsClient;
    static Timer heartbeatTimer;

    public WebSocketClientEndpoint(URI serverURI) {

        System.out.println("[Nifty] WebSocket Class Initialized");

        wsClient = new WebSocketClient(serverURI) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                JSONObject identify = new JSONObject();
                identify.put("operation", "identify");

                JSONObject data = new JSONObject();
                data.put("token", "sEQX79AGT5RerI50Vi1jh949Ji");

                identify.put("data", data);

                send(identify.toString());
            }

            @Override
            public void onMessage(String message) {
                JSONObject json = new JSONObject(message);

                if (Objects.equals(json.getString("operation"), "hello")) {
                    heartbeat(json.getJSONObject("data").getInt("heartbeatInterval"));
                }

                if (Objects.equals(json.getString("operation"), "update_guild")) {

                    long userId = json.getJSONObject("data").getLong("userId");
                    Long guildId = WsGuild.getGuildForUserId(userId);

                    WsGuild.setGuildForUserId(userId, guildId);
                    return;

                }

                if (Objects.equals(json.getString("operation"), "action")) {

                    long guildId = json.getJSONObject("data").getLong("guildId");
                    String action = json.getJSONObject("data").getString("action");

                    PlayerManager playerManager = PlayerManager.get(guildId);

                    if (action.equals("refresh_player")) {
                        WsPlayer.updateWsPlayer(playerManager);
                        return;
                    };

                    if (playerManager == null) {
                        return;
                    }

                    if (action.equals("togglePause")) {
                        playerManager.getAudioPlayer().setPaused(!playerManager.getAudioPlayer().isPaused());
                        return;
                    };

                    if (action.equals("back")) {
                        playerManager.getTrackScheduler().back();
                        return;
                    };

                    if (action.equals("skip")) {
                        playerManager.getTrackScheduler().skip();
                        return;
                    };

                    if (action.equals("loop")) {
                        Loop currentLoop = playerManager.getPlayerHandler().getLoopMode();

                        if (currentLoop == Loop.DISABLED) {
                            playerManager.getPlayerHandler().setLoopMode(Loop.QUEUE);
                        } else if (currentLoop == Loop.QUEUE) {
                            playerManager.getPlayerHandler().setLoopMode(Loop.TRACK);
                        } else {
                            playerManager.getPlayerHandler().setLoopMode(Loop.DISABLED);
                        }

                        WsPlayer.updateWsPlayer(playerManager);
                        return;
                    }

                    if (action.equals("shuffle")) {
                        Shuffle currentShuffle = playerManager.getPlayerHandler().getShuffleMode();

                        if (currentShuffle == Shuffle.DISABLED) {
                            playerManager.getPlayerHandler().setShuffleMode(Shuffle.ENABLED);
                            int currentPosition = playerManager.getPlayerHandler().getPosition();
                            playerManager.getQueueHandler().shuffleAfter(currentPosition + 1);
                        } else {
                            playerManager.getPlayerHandler().setShuffleMode(Shuffle.DISABLED);
                        }

                        WsPlayer.updateWsPlayer(playerManager);
                        return;
                    }

                    if (action.equals("volume")) {
                        playerManager.getAudioPlayer().setVolume(json.getJSONObject("data").getInt("volume"));
                        WsPlayer.updateWsPlayer(playerManager);
                        return;
                    }

                    if (action.equals("jump")) {
                        playerManager.getTrackScheduler().jump(json.getJSONObject("data").getInt("trackId"));
                        return;
                    }

                    AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();

                    if (playingTrack == null) {
                        return;
                    }

                    if (action.equals("seek")) {
                        playingTrack.setPosition(json.getJSONObject("data").getLong("position"));
                        WsPlayer.updateWsPlayer(playerManager);
                    }

                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

                heartbeatTimer.cancel();
                heartbeatTimer.purge();

                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                wsClient.reconnect();
                            }
                        },
                        10000
                );

            }

            @Override
            public void onError(Exception ex) {

            }
        };

        wsClient.connect();

    }

    public static void send(String message) {
        if (wsClient.isOpen()) {
            wsClient.send(message);
        }
    }

    public void heartbeat(int interval) {

        // create a timer to send a heartbeat every interval
        heartbeatTimer = new Timer();

        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("operation", "heartbeat");
                WebSocketClientEndpoint.send(heartbeat.toString());
            }
        }, 0, interval);

    }

}
