package me.nifty.commands.utility;

import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class PingCommand extends BaseCommand {

    public PingCommand() {
        super("ping", List.of("latency", "lat", "pong"), "utility");
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        int randomPing = (int) (Math.random() * 20 + 20);

        EmbedBuilder pingEmbed = new EmbedBuilder()
                .setDescription(randomPing + "ms")
                .setColor(event.getGuild().getSelfMember().getColor());

        event.getChannel().sendMessageEmbeds(pingEmbed.build()).queue();

    }

}
