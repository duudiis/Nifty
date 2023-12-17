package me.nifty.events;

import me.nifty.utils.ReconnectUtils;
import me.nifty.utils.SlashUtils;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Ready extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {

        System.out.println("[Nifty] Logged in as " + event.getJDA().getSelfUser().getName());

        // Reconnect players to their voice channels after a restart
        ReconnectUtils.reconnectPlayers(event.getJDA());

    }

}