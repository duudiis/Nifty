package me.nifty.structures;

import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BaseAutoComplete {

    private final String name;

    /**
     * Creates a new BaseAutoComplete.
     *
     * @param name The name of the button.
     */
    public BaseAutoComplete(String name) {
        this.name = name;
    }

    /**
     * Executes the auto complete.
     *
     * @param event The event.
     */
    public void execute(CommandAutoCompleteInteractionEvent event) {

        event.replyChoices(Collections.emptyList()).queue();

    }

    /**
     * Returns the name of the auto complete.
     *
     * @return The name of the auto complete.
     */
    public String getName() {
        return name;
    }

}
