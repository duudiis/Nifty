package me.nifty;

import me.nifty.managers.DatabaseManager;
import me.nifty.managers.JDAManager;

public class Nifty {

    /**
     * The main method
     * @param args The arguments
     */
    public static void main(String[] args) {

        System.out.println("[Nifty] Booting up...");

        // Connects to the MySQL Server
        DatabaseManager.connect();

        // Creates the JDA Instance
        JDAManager.create();

    }

}