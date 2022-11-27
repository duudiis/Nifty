package me.nifty.utils.formatting;

import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

public class QueueButtons {

    /**
     * Returns a list of buttons for the queue command.
     *
     * @param page The page of the queue to display.
     * @return A list of buttons for the queue command.
     */
    public static List<Button> get(int page) {

        // Creates a list of buttons
        List<Button> buttonsRow = new ArrayList<>();

        // Adds the buttons to the list

        Button button = Button.secondary("queue_0_first", "First");
        buttonsRow.add(button);

        button = Button.secondary("queue_" + (page - 1), "Back");
        buttonsRow.add(button);

        button = Button.secondary("queue_" + (page + 1), "Next");
        buttonsRow.add(button);

        button = Button.secondary("queue_-2_last", "Last");
        buttonsRow.add(button);

        // Returns the list of buttons
        return buttonsRow;

    }

}
