package me.nifty.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import kotlin.Pair;
import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.formatting.TrackTime;
import me.nifty.utils.formatting.TrackTitle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class NowPlayingCommand extends BaseCommand {

    public NowPlayingCommand() {
        super("now", List.of("np", "playing", "nowplaying"), "music");

        this.requiresVoice = false;

        this.deferReply = false;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        Pair<Boolean, MessageEmbed> result = nowPlayingCommand(event.getGuild());

        MessageEmbed embed = result.getSecond();

        event.getChannel().sendMessageEmbeds(embed).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        Pair<Boolean, MessageEmbed> result = nowPlayingCommand(event.getGuild());

        Boolean success = result.getFirst();
        MessageEmbed embed = result.getSecond();

        if (success) {
            event.replyEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }

    }

    /**
     * Gets the now playing track from the guild's player and formats it into an embed.
     *
     * @param guild The guild to get the now playing track from
     * @return Pair with the success of the command and the message embed to return.
     */
    private Pair<Boolean, MessageEmbed> nowPlayingCommand(Guild guild) {

        PlayerManager playerManager = PlayerManager.get(guild);

        if (playerManager == null || playerManager.getAudioPlayer().getPlayingTrack() == null) {
            return new Pair<>(false, ErrorEmbed.get("You must be playing a track to use this command!"));
        }

        AudioTrack playingTrack = playerManager.getAudioPlayer().getPlayingTrack();

        String title = TrackTitle.format(playingTrack, 63);

        long position = playingTrack.getPosition();
        long duration = playingTrack.getDuration();

        String progressBar = TrackTime.formatBar(position, duration);
        String time = TrackTime.formatNatural(position) + " / " + TrackTime.formatNatural(duration);

        EmbedBuilder nowPlayingEmbed = new EmbedBuilder()
                .setDescription("[" + title + "](" + playingTrack.getInfo().uri + ") [<@!" + playingTrack.getUserData() + ">]")
                .setColor(guild.getSelfMember().getColor())
                .setFooter(progressBar + " " + time);

        return new Pair<>(true, nowPlayingEmbed.build());

    }

}
