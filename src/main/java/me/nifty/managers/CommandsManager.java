package me.nifty.managers;

import me.nifty.commands.configuration.PrefixCommand;
import me.nifty.commands.music.*;
import me.nifty.commands.utility.PingCommand;
import me.nifty.structures.BaseCommand;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CommandsManager {

    private static final Map<String, BaseCommand> commands = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();

    /**
     * Loads all commands into the commands map.
     */
    public static void load() {

        // Configuration Commands
        registerCommand(new PrefixCommand());

        // Music Commands
        registerCommand(new AutoplayCommand());
        registerCommand(new BackCommand());
        registerCommand(new ClearCommand());
        registerCommand(new DisconnectCommand());
        registerCommand(new FastForwardCommand());
        registerCommand(new JoinCommand());
        registerCommand(new JumpCommand());
        registerCommand(new LoopCommand());
        registerCommand(new MoveCommand());
        registerCommand(new NowPlayingCommand());
        registerCommand(new PauseCommand());
        registerCommand(new PlayCommand());
        registerCommand(new QueueCommand());
        registerCommand(new RemoveCommand());
        registerCommand(new RewindCommand());
        registerCommand(new SeekCommand());
        registerCommand(new SkipCommand());
        registerCommand(new StopCommand());
        registerCommand(new UnpauseCommand());

        // Utility Commands
        registerCommand(new PingCommand());

    }

    /**
     * Registers a command to the commands map.
     *
     * @param command The command to register.
     */
    public static void registerCommand(BaseCommand command) {

        // If the command already exists, print an error message.
        if (commands.containsKey(command.getName())) {
            System.out.println("Command " + command.getName() + " already exists!");
            return;
        }

        // Add the command to the commands map.
        commands.put(command.getName(), command);

        // Add the command's aliases to the aliases map.
        for (String alias : command.getAliases()) {

            // If the alias already exists, print an error message.
            if (aliases.containsKey(alias)) {
                System.out.println("Alias " + alias + " from " + command.getName() + " has already been registered for " + aliases.get(alias) + "!");
                continue;
            }

            // Add the alias to the aliases map.
            aliases.put(alias, command.getName());

        }

    }

    /**
     * Gets a command from the command map.
     *
     * @param query The name or alias of the command.
     * @return The command, or null if it doesn't exist.
     */
    @Nullable
    public static BaseCommand getCommand(String query) {

        // Check if the query is a command name.
        if (commands.containsKey(query)) {
            return commands.get(query);
        }

        // Check if the query is an alias.
        if (aliases.containsKey(query)) {
            return commands.get(aliases.get(query));
        }

        return null;

    }

}
