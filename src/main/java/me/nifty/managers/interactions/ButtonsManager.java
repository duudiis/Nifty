package me.nifty.managers.interactions;

import me.nifty.interactions.buttons.QueueButton;
import me.nifty.structures.BaseButton;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ButtonsManager {

    private static final Map<String, BaseButton> buttons = new HashMap<>();

    /**
     * Loads all buttons into the buttons map.
     */
    public static void load() {

        // Buttons
        registerButton(new QueueButton());

    }

    /**
     * Registers a button to the buttons map.
     *
     * @param button The button to register.
     */
    public static void registerButton(BaseButton button) {

        // Checks if a button with this name already exists
        if (buttons.containsKey(button.getName())) {
            System.out.println("Button " + button.getName() + " already exists!");
            return;
        }

        // Adds the button to the map
        buttons.put(button.getName(), button);

    }

    /**
     * Gets a button from the buttons map.
     *
     * @param name The name of the button.
     * @return The button.
     */
    @Nullable
    public static BaseButton getButton(String name) {
        return buttons.get(name);
    }

}
