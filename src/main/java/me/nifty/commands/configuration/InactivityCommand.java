package me.nifty.commands.configuration;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import kotlin.Pair;
import me.nifty.core.database.guild.GuildHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.InactivityUtils;
import me.nifty.utils.enums.InactivityType;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.parser.BoolParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.List;
import java.util.Objects;

public class InactivityCommand extends BaseCommand {

    public InactivityCommand() {
        super("247", List.of("24/7", "forever", "stay", "infinity", "infinite"), "configuration");

        this.requiresVoice = false;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = inactivityCommand(event.getGuild(), Objects.requireNonNull(event.getMember()), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = inactivityCommand(Objects.requireNonNull(event.getGuild()), Objects.requireNonNull(event.getMember()), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Enables/disables the 24/7 mode feature for the guild.
     *
     * @param guild The guild to execute the command in
     * @param input The input to parse
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> inactivityCommand(Guild guild, Member member, String input) {

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            return new Pair<>(false, ErrorEmbed.get("You must have the `Manage Server` permission to use this command!"));
        }

        Boolean currentInactivity = GuildHandler.getInactivityDisconnect(guild.getIdLong());
        boolean newInactivity = BoolParser.parse(input, currentInactivity);

        if (currentInactivity != newInactivity) {
            if (newInactivity) { onInactivityDisable(guild); } else { onInactivityEnable(guild); }
        }

        GuildHandler.setInactivityDisconnect(guild.getIdLong(), newInactivity);

        EmbedBuilder inactivityEmbed = new EmbedBuilder()
                .setDescription("24/7 mode is now **" + (newInactivity ? "enabled" : "disabled") + "** in this server.")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, inactivityEmbed.build());

    }

    /**
     * When 24/7 mode is disabled, this method is called to
     * update the timers to inactivity disconnect.
     *
     * @param guild The guild to enable the feature in
     */
    private void onInactivityEnable(Guild guild) {

        AudioManager JDAAudioManager = guild.getAudioManager();

        if (JDAAudioManager.isConnected() && JDAAudioManager.getConnectedChannel() != null) {

            VoiceChannel voiceChannel = JDAAudioManager.getConnectedChannel().asVoiceChannel();

            int membersInChannel = voiceChannel.getMembers().stream().filter(member -> !member.getUser().isBot()).toList().size();

            if (membersInChannel == 0) {
                InactivityUtils.startTimer(InactivityType.ALONE, guild);
            }

        }

        PlayerManager playerManager = PlayerManager.get(guild);

        if (playerManager != null) {

            AudioPlayer audioPlayer = playerManager.getAudioPlayer();

            if (audioPlayer.getPlayingTrack() == null) {
                InactivityUtils.startTimer(InactivityType.STOPPED, guild);
            }

            if (audioPlayer.isPaused()) {
                InactivityUtils.startTimer(InactivityType.PAUSED, guild);
            }

        }

    }

    /**
     * When 24/7 mode is enabled, this method is called to
     * disable the timers for inactivity disconnect.
     *
     * @param guild The guild to disable the feature in
     */
    private void onInactivityDisable(Guild guild) {

        InactivityUtils.stopTimer(InactivityType.PAUSED, guild);
        InactivityUtils.stopTimer(InactivityType.STOPPED, guild);
        InactivityUtils.stopTimer(InactivityType.ALONE, guild);

    }

}
