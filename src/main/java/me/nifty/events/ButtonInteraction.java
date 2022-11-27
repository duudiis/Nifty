package me.nifty.events;

import me.nifty.managers.interactions.ButtonsManager;
import me.nifty.structures.BaseButton;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonInteraction extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        // Gets the button id array.
        String[] idArray = event.getComponentId().split("_");

        // Gets the button name.
        String name = idArray[0];

        // Gets the button and checks if it exists.
        BaseButton button = ButtonsManager.getButton(name);
        if (button == null) { return; }

        // If the button requires deferReply, defers the reply.
        if (button.isDeferReply()) {
            event.deferReply(button.isEphemeral()).queue();
        }

        // If the button requires deferUpdate, defers the update.
        if (button.isDeferUpdate()) {
            event.deferEdit().queue();
        }

        // Gets the button arguments.
        String[] args = new String[idArray.length - 1];
        System.arraycopy(idArray, 1, args, 0, idArray.length - 1);

        // Executes the button.
        button.execute(event, args);

    }

}
