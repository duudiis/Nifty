package me.nifty.commands.utility;

import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class PingCommand extends BaseCommand {

    public PingCommand() {
        super("ping", List.of("latency", "lat", "pong"), "utility");
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        event.getChannel().sendMessage("Pong!").queue();

    }

}
