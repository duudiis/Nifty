package me.nifty.interactions.autocomplete;

import me.nifty.structures.BaseAutoComplete;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchAutoComplete extends BaseAutoComplete {

    public SearchAutoComplete() {
        super("search");
    }

    @Override
    public void execute(CommandAutoCompleteInteractionEvent event) {

        if (event.getFocusedOption().getValue().isEmpty()) {
            event.replyChoices(Collections.emptyList()).queue();
            return;
        }

        try {
            List<Command.Choice> choices = autoCompleteResults(event.getFocusedOption().getValue());
            event.replyChoices(choices).queue();
        } catch (Exception ignored) {
            event.replyChoices(Collections.emptyList()).queue();
        }

    }

    private List<Command.Choice> autoCompleteResults(String query) throws IOException {
        String url = "https://clients1.google.com/complete/search?client=youtube&hl=en&gs_rn=64&gs_ri=youtube&ds=yt&cp=10&gs_id=b2&q=";
        String re = "\\[\"(.*?)\",";

        Connection.Response resp = Jsoup.connect(url + URLEncoder.encode(query, StandardCharsets.UTF_8)).execute();
        Matcher match = Pattern.compile(re, Pattern.DOTALL).matcher(resp.body());

        List<Command.Choice> youtubeAutoComplete = new ArrayList<>();

        while (match.find()) {
            youtubeAutoComplete.add(new Command.Choice(match.group(1), match.group(1)));
        }

        return youtubeAutoComplete;
    }

}
