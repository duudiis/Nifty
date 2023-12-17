package me.nifty.managers.interactions;

import me.nifty.interactions.autocomplete.SearchAutoComplete;
import me.nifty.interactions.autocomplete.PlayAutoComplete;
import me.nifty.structures.BaseAutoComplete;

import java.util.HashMap;
import java.util.Map;

public class AutoCompleteManager {

    private static final Map<String, BaseAutoComplete> autoCompletes = new HashMap<>();

    /**
     * Loads all auto completes into the auto completes map.
     */
    public static void load() {

        registerAutoComplete(new SearchAutoComplete());
        registerAutoComplete(new PlayAutoComplete());

    }

    /**
     * Registers an auto complete to the auto completes map.
     *
     * @param autoComplete The auto complete to register.
     */
    public static void registerAutoComplete(BaseAutoComplete autoComplete) {

        // Checks if an auto complete with this name already exists
        if (autoCompletes.containsKey(autoComplete.getName())) {
            System.out.println("Auto complete " + autoComplete.getName() + " already exists!");
        }

        autoCompletes.put(autoComplete.getName(), autoComplete);

    }

    /**
     * Gets an auto complete from the auto completes map.
     *
     * @param name The name of the auto complete.
     * @return The auto complete.
     */
    public static BaseAutoComplete getAutoComplete(String name) {
        return autoCompletes.get(name);
    }

}
