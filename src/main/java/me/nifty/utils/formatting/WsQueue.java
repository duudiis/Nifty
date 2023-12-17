package me.nifty.utils.formatting;

import me.nifty.websocket.WebSocketClientEndpoint;
import org.json.JSONObject;

public class WsQueue {

    public static void updateWsQueue(long guildId) {

        JSONObject base = new JSONObject();
        base.put("operation", "refresh_queue");

        JSONObject data = new JSONObject();
        data.put("guildId", String.valueOf(guildId));

        base.put("data", data);

        WebSocketClientEndpoint.send(base.toString());

    }

}
