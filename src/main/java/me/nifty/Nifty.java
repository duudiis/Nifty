package me.nifty;

import me.nifty.managers.DatabaseManager;
import me.nifty.managers.JDAManager;

import java.util.Arrays;

public class Nifty {

    private static boolean allowMessageCommands = true;

    /**
     * The main method
     * @param args The arguments
     */
    public static void main(String[] args) {

        System.out.println("[Nifty] Booting up...");

        if (Arrays.asList(args).contains("--disable-message-commands")) {
            allowMessageCommands = false;
            System.out.println("[Nifty] Warning: Message commands are disabled");
        }

        // Connects to the MySQL Server
        DatabaseManager.connect();

        // Creates the JDA Instance
        JDAManager.create();

    }
    public static boolean allowMessageCommands() {
        return allowMessageCommands;
    }

}