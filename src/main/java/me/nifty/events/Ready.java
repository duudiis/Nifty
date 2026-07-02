package me.nifty.events;

import me.nifty.core.database.BotIdentity;
import me.nifty.managers.WebSocketManager;
import me.nifty.utils.ReconnectUtils;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Ready extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {

        System.out.println("[Nifty] Logged in as " + event.getJDA().getSelfUser().getName());

        // Registers this bot instance in the shared database (players, queues
        // and settings are scoped by its id) — must happen before anything
        // touches the database on this bot's behalf.
        BotIdentity.initialize(event.getJDA().getSelfUser());

        // Connects the optional dashboard add-on now that the bot id is known
        // (no-op if DASHBOARD_WS_URL is unset or already connected)
        WebSocketManager.connect();

        // Reconnect players to their voice channels after a restart
        ReconnectUtils.reconnectPlayers(event.getJDA());

    }

}