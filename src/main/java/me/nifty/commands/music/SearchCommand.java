package me.nifty.commands.music;

import me.nifty.core.database.guild.PrefixHandler;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.CommandsManager;
import me.nifty.structures.BaseCommand;
import me.nifty.utils.formatting.ErrorEmbed;
import me.nifty.utils.parser.QueryParser;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

public class SearchCommand extends BaseCommand {

    public SearchCommand() {
        super("search", List.of("s", "sea", "se", "src"), "music");

        this.requiresVoice = true;

        this.deferReply = true;
        this.ephemeral = false;
    }

    @Override
    public void executeAsMessage(MessageReceivedEvent event, String[] args) {

        String query = String.join(" ", args);

        if (query.isEmpty()) {
            return;
        }

        QueryParser parsedQuery = QueryParser.parse(query + " -c");

        PlayerManager playerManager = PlayerManager.get(event.getGuild());

        playerManager.getTrackScheduler().queue(parsedQuery.getQuery(), event.getChannel().asGuildMessageChannel(), event.getMember(), parsedQuery.getFlags());

    }

    @Override
    public void executeAsSlashCommand(SlashCommandInteractionEvent event) {

        String query = Objects.requireNonNull(event.getOption("input")).getAsString();

        QueryParser parsedQuery = QueryParser.parse(query + " -c");

        PlayerManager playerManager = PlayerManager.get(Objects.requireNonNull(event.getGuild()));

        playerManager.getTrackScheduler().queue(parsedQuery.getQuery(), event.getHook(), event.getMember(), parsedQuery.getFlags());

    }

}