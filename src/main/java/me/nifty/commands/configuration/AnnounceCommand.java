package me.nifty.commands.configuration;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import kotlin.Pair;
import me.nifty.core.database.guild.GuildHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.NowPlayingEmbed;
import me.nifty.utils.parser.BoolParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AnnounceCommand extends BaseCommand {

    public AnnounceCommand() {
        super("announce", List.of("an", "ann", "announces", "announcements"), "configuration");

        this.requiresVoice = false;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        Pair<Boolean, MessageEmbed> result = announceCommand(event.getGuild(), Objects.requireNonNull(event.getMember()), query);

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = announceCommand(Objects.requireNonNull(event.getGuild()), Objects.requireNonNull(event.getMember()), "");

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Enables/disables the announcement of the currently playing song in the text channel.
     *
     * @param guild The guild to execute the command in
     * @param input The input to parse
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> announceCommand(Guild guild, Member member, String input) {

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            return new Pair<>(false, ErrorEmbed.get("You must have the `Manage Server` permission to use this command!"));
        }

        Boolean currentAnnouncementsMode = GuildHandler.getAnnouncementsMode(guild.getIdLong());
        boolean newAnnouncementsMode = BoolParser.parse(input, currentAnnouncementsMode);

        GuildHandler.setAnnouncementsMode(guild.getIdLong(), newAnnouncementsMode);

        EmbedBuilder announceEmbed = new EmbedBuilder()
                .setDescription("**Announcing of tracks** is now **" + (newAnnouncementsMode ? "enabled" : "disabled") + "**.")
                .setColor(guild.getSelfMember().getColor());

        return new Pair<>(true, announceEmbed.build());

    }

}
