package me.nifty.events;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Ready extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {

        System.out.println("[Nifty] Logged in as " + event.getJDA().getSelfUser().getName());

    }

}