package me.nifty.commands.music;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.QueueButtons;
import me.nifty.utils.formatting.QueueMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class QueueCommand extends BaseCommand {

    public QueueCommand() {
        super("queue", List.of("q", "que", "list", "tracks", "line"), "music");

        this.requiresVoice = false;

        this.deferReply = true;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        PlayerManager playerManager = PlayerManager.get(event.getGuild());

        if (playerManager == null || playerManager.getQueueHandler().getQueueSize() == 0) {
            List<Button> buttonsRow = QueueButtons.get(0);

            event.getChannel().sendMessage("```nim\nThe queue is empty ;-;```").addActionRow(buttonsRow).queue();
            return;
        }

        int currentPosition = playerManager.getPlayerHandler().getPosition();

        int page = currentPosition / 10;

        String queueMessage = QueueMessage.get(playerManager, page);
        List<Button> buttonsRow = QueueButtons.get(page);

        event.getChannel().sendMessage(queueMessage).addActionRow(buttonsRow).queue();

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));

        if (playerManager == null || playerManager.getQueueHandler().getQueueSize() == 0) {
            List<Button> buttonsRow = QueueButtons.get(0);

            event.getHook().sendMessage("```nim\nThe queue is empty ;-;```").addActionRow(buttonsRow).queue();
            return;
        }

        int currentPosition = playerManager.getPlayerHandler().getPosition();

        int page = currentPosition / 10;

        String queueMessage = QueueMessage.get(playerManager, page);
        List<Button> buttonsRow = QueueButtons.get(page);

        event.getHook().sendMessage(queueMessage).addActionRow(buttonsRow).queue();

    }

    @Override
    public void executeAsVoice(String arguments, Guild guild, User user) {

        PlayerManager playerManager = PlayerManager.get(guild);

        Member member = guild.getMember(user);
        System.out.println("Queueing from voice: " + arguments);
        playerManager.getTrackScheduler().queue("ytsearch:" + arguments, null, member, List.of());

    }

}
