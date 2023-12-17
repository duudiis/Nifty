package me.nifty.events;

import me.nifty.Nifty;
import me.nifty.core.database.guild.PrefixHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.CommandsManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.validations.voice.VoiceConnection;
import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Objects;

public class MessageReceived extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) { return; }
        if (event.getChannelType() == ChannelType.PRIVATE) { return; }

        String selfMention = event.getJDA().getSelfUser().getAsMention();
        String guildMention = event.getGuild().getSelfMember().getAsMention();

        String content = event.getMessage().getContentRaw().replace(guildMention, selfMention);

        if (content.equals(selfMention)) {
            getStarted(event);
            return;
        }

        String prefix = content.startsWith(selfMention) ? selfMention : PrefixHandler.getPrefix(event.getGuild().getIdLong());

        if (!content.startsWith(prefix)) { return; }

        String[] messageArray = content.substring(prefix.length()).trim().split(" +");
        String commandName = messageArray[0].toLowerCase();

        BaseCommand command = CommandsManager.getCommand(commandName);
        if (command == null) { return; }

        if (!Nifty.allowMessageCommands() && !event.getMember().getId().equals("676156762457374742")) {
            event.getChannel().sendMessageEmbeds(ErrorEmbed.get("Message commands have been temporarily disabled.\nPlease use slash commands instead.")).queue();
            return;
        }

        // TODO: Validate text channel permissions

        if (command.requiresVoice()) {
            GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
            GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

            String voiceErrorMessage = VoiceConnection.validate(memberVoiceState, selfVoiceState);

            if (voiceErrorMessage != null) {
                event.getChannel().sendMessageEmbeds(ErrorEmbed.get(voiceErrorMessage)).queue();
                return;
            }
        }

        if (command.getCategory().equals("music")) {

            PlayerManager playerManager = PlayerManager.get(event.getGuild());

            if (playerManager != null) {
                playerManager.getPlayerHandler().setTextChannelId(event.getChannel().getIdLong());
            }

        }

        String[] args = new String[messageArray.length - 1];
        System.arraycopy(messageArray, 1, args, 0, messageArray.length - 1);

        command.executeAsMessage(event, args);

    }

    private void getStarted(MessageReceivedEvent event) {

        EmbedBuilder getStartedEmbed = new EmbedBuilder();

        if (Objects.requireNonNull(event.getMember()).getPermissions().contains(Permission.USE_APPLICATION_COMMANDS)) {
            getStartedEmbed.setDescription("You can play music by joining a voice channel and typing `/play`. The command accepts song names, video links, and playlist links.");
        } else {
            String prefix = PrefixHandler.getPrefix(event.getGuild().getIdLong());
            getStartedEmbed.setDescription("You can play music by joining a voice channel and typing `" + prefix + "play`. The command accepts song names, video links, and playlist links.");
        }

        getStartedEmbed.setColor(Color.decode("#202225"));

        event.getChannel().sendMessageEmbeds(getStartedEmbed.build()).queue();

    }

}
