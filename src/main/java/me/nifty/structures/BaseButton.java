package me.nifty.structures;

import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class BaseButton {

    private final String name;

    protected boolean deferReply = false;
    protected boolean deferUpdate = false;
    protected boolean ephemeral = true;

    /**
     * Creates a new BaseButton.
     *
     * @param name The name of the button.
     */
    public BaseButton(String name) {
        this.name = name;
    }

    /**
     * Executes the button.
     *
     * @param event The event.
     */
    public void execute(ButtonInteractionEvent event, String[] args) {

        MessageEmbed errorEmbed = ErrorEmbed.get("This button is not available.");

        if (deferReply) {
            event.getHook().sendMessageEmbeds(errorEmbed).queue();
            return;
        }

        if (deferUpdate) {
            event.getHook().editOriginalEmbeds(errorEmbed).queue();
            return;
        }

        event.replyEmbeds(errorEmbed).setEphemeral(ephemeral).queue();

    }

    /**
     * Returns the name of the button.
     *
     * @return The name of the button.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the button should defer the reply.
     *
     * @return Whether the button should defer the reply.
     */
    public boolean isDeferReply() {
        return deferReply;
    }

    /**
     * Returns whether the button should defer the update.
     *
     * @return Whether the button should defer the update.
     */
    public boolean isDeferUpdate() {
        return deferUpdate;
    }

    /**
     * Returns whether the button should be ephemeral.
     *
     * @return Whether the button should be ephemeral.
     */
    public boolean isEphemeral() {
        return ephemeral;
    }

}
