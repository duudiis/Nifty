package me.nifty.events;

import me.nifty.managers.interactions.AutoCompleteManager;
import me.nifty.structures.BaseAutoComplete;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class AutoCompleteInteraction extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {

        String name = event.getName();

        BaseAutoComplete autoComplete = AutoCompleteManager.getAutoComplete(name);
        if (autoComplete == null) { return; }

        autoComplete.execute(event);

    }

}