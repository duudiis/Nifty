package me.nifty.commands.utility;

import me.nifty.core.database.guild.PrefixHandler;
import me.nifty.structures.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class HelpCommand extends BaseCommand {

    public HelpCommand() {
        super("help", List.of("h", "assist", "support"), "utility");
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        MessageEmbed helpEmbed = getHelpEmbed(event.getGuild());
        event.getChannel().sendMessageEmbeds(helpEmbed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        MessageEmbed helpEmbed = getHelpEmbed(Objects.requireNonNull(event.getGuild()));
        event.replyEmbeds(helpEmbed).queue();

    }

    private MessageEmbed getHelpEmbed(Guild guild) {

        User selfUser = guild.getSelfMember().getUser();
        String selfUsername = selfUser.getName();

        String prefix = PrefixHandler.getPrefix(guild.getIdLong());

        EmbedBuilder helpEmbed = new EmbedBuilder()
                .setAuthor(selfUsername, null, selfUser.getAvatarUrl())
                .setDescription(selfUsername + " is the easiest way to play music in your Discord server. It supports Spotify, YouTube, Soundcloud and more!\n\nTo get started, join a voice channel and `" + prefix + "play` a song. You can use song names, video links, and playlist links.\n\u1CBC")
                .addField("Invite", selfUsername + " can be added to as many servers as you want! [Click here to add it to yours.](https://discord.com/oauth2/authorize?client_id=" + selfUser.getId() + "&permissions=8&scope=bot%20applications.commands)", false)
                .setColor(guild.getSelfMember().getColor());

        return helpEmbed.build();

    }

}
