package me.nifty.commands.configuration;

import me.nifty.core.database.guild.PrefixHandler;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class PrefixCommand extends BaseCommand {

    public PrefixCommand() {
        super("prefix", List.of("pre", "pr"), "configuration");
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String currentPrefix = PrefixHandler.getPrefix(event.getGuild().getIdLong());

        if (args.length == 0) {

            EmbedBuilder prefixEmbed = new EmbedBuilder()
                    .setDescription("This server's prefix is **" + currentPrefix + "**")
                    .setColor(event.getGuild().getSelfMember().getColor());

            event.getChannel().sendMessageEmbeds(prefixEmbed.build()).queue();
            return;

        }

        String newPrefix = args[0];

        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.MANAGE_SERVER)) {
            event.getChannel().sendMessageEmbeds(ErrorEmbed.get("You must have the `Manage Server` permission to use this command!")).queue();
            return;
        }

        if (newPrefix.length() > 16) {
            event.getChannel().sendMessageEmbeds(ErrorEmbed.get("The prefix must be 16 characters or less!")).queue();
            return;
        }

        boolean prefixUpdated = PrefixHandler.setPrefix(event.getGuild().getIdLong(), newPrefix);

        if (!prefixUpdated) {
            event.getChannel().sendMessageEmbeds(ErrorEmbed.get("An error occurred while updating the prefix.")).queue();
            return;
        }

        EmbedBuilder prefixEmbed = new EmbedBuilder()
                .setDescription("This server's prefix is now **" + newPrefix + "**. Commands must now use **" + newPrefix + "** as their prefix. For example, `" + newPrefix + "play`.")
                .setColor(event.getGuild().getSelfMember().getColor());

        event.getChannel().sendMessageEmbeds(prefixEmbed.build()).queue();

    }

}
