package me.nifty.events;

import me.nifty.core.music.PlayerManager;
import me.nifty.managers.CommandsManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.validations.voice.VoiceConnection;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Objects;

public class SlashCommandInteraction extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            event.replyEmbeds(ErrorEmbed.get("This command can only be run in a server!")).queue();
            return;
        }

        String commandName = event.getName();

        BaseCommand command = CommandsManager.getCommand(commandName);
        if (command == null) { return; }

        // TODO: Validate text channel permissions

        if (command.requiresVoice()) {
            GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
            GuildVoiceState selfVoiceState = Objects.requireNonNull(event.getGuild()).getSelfMember().getVoiceState();

            String voiceErrorMessage = VoiceConnection.validate(memberVoiceState, selfVoiceState);

            if (voiceErrorMessage != null) {
                event.replyEmbeds(ErrorEmbed.get(voiceErrorMessage)).setEphemeral(true).queue();
                return;
            }
        }

        if (command.getCategory().equals("music")) {

            PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));

            if (playerManager != null) {
                playerManager.getPlayerHandler().setTextChannelId(event.getChannel().getIdLong());
            }

        }

        if (command.requiresDeferReply()) {
            event.deferReply(command.isEphemeral()).queue();
        }

        command.executeAsSlashCommand(event);

    }

}
