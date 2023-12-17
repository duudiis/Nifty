package me.nifty.managers;

import me.nifty.websocket.WebSocketClientEndpoint;

import java.net.URI;

public class WebSocketManager {

    public static void connect() {

        try {
            new WebSocketClientEndpoint(new URI("ws://192.168.15.151/"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}