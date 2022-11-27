package me.nifty.interactions.buttons;

import me.nifty.core.music.PlayerManager;
import me.nifty.structures.BaseButton;
import me.nifty.utils.formatting.QueueButtons;
import me.nifty.utils.formatting.QueueMessage;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.Objects;

public class QueueButton extends BaseButton {

    public QueueButton() {
        super("queue");

        this.deferReply = false;
        this.deferUpdate = true;
        this.ephemeral = false;
    }

    @Override
    public void execute(ButtonInteractionEvent event, String[] args) {

        // Gets the player.
        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));

        // If the player doesn't exist or the queue is empty, return.
        if (playerManager == null || playerManager.getQueueHandler().getQueueSize() == 0) {
            List<Button> buttonsRow = QueueButtons.get(0);

            event.getHook().editOriginal("```nim\nThe queue is empty ;-;```").setActionRow(buttonsRow).queue();
            return;
        }

        // Gets the queue page.
        int page = Integer.parseInt(args[0]);

        // Gets the queue max page.
        int queueSize = playerManager.getQueueHandler().getQueueSize();
        int queueMax = (queueSize % 10 == 0) ? ((queueSize / 10) - 1) : (queueSize / 10);

        // If the page is greater than the max page or page is the "Last" button, sets the page to the max page.
        if (page > queueMax || page == -2) {
            page = queueMax;
        }

        // If the page is less than 0, sets the page to 0.
        if (page < 0) {
            page = 0;
        }

        // Gets the queue message and buttons.
        String queueMessage = QueueMessage.get(playerManager, page);
        List<Button> buttonsRow = QueueButtons.get(page);

        event.getHook().editOriginal(queueMessage).setActionRow(buttonsRow).queue();

    }

}
