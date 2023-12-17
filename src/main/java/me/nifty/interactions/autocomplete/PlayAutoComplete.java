package me.nifty.interactions.autocomplete;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.managers.AudioManager;
import me.nifty.structures.BaseAutoComplete;
import me.nifty.utils.formatting.TrackTime;
import me.nifty.utils.formatting.TrackTitle;
import me.nifty.utils.parser.QueryParser;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayAutoComplete extends BaseAutoComplete {

    private final AudioPlayerManager audioManager = AudioManager.getAudioManager();

    public PlayAutoComplete() {
        super("play");
    }

    @Override
    public void execute(CommandAutoCompleteInteractionEvent event) {

        if (event.getFocusedOption().getValue().isEmpty()) {
            event.replyChoices(
                    new Command.Choice(
                            "Type a song name, artist, or link to play a song.",
                            "https://www.youtube.com/watch?v=ntthrYgpOKY"
                    )
            ).queue();
            return;
        }

        try {
            List<Command.Choice> choices = autoCompleteResults(event.getFocusedOption().getValue());
            event.replyChoices(choices).queue();
        } catch (Exception ignored) {
            event.replyChoices(Collections.emptyList()).queue();
        }

    }

    private List<Command.Choice> autoCompleteResults(String query) {

        CompletableFuture<List<Command.Choice>> searchAutoComplete = new CompletableFuture<>();

        QueryParser parsedQuery = QueryParser.parse(query);

        audioManager.loadItem(parsedQuery.getQuery().replace("ytsearch:", "ytmsearch:"), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {

                List<Command.Choice> choices = Collections.singletonList(
                        new Command.Choice(
                                TrackTitle.format(track, 64)+ " \u00B7 " + TrackTime.formatClock(track.getDuration()) +
                                        (parsedQuery.getFlags().toArray().length > 0 ? " \u00B7 " + "Flags: " + parsedQuery.getStringFlags() : ""),
                                track.getInfo().uri + " " + parsedQuery.getStringFlags()
                        )
                );

                searchAutoComplete.complete(choices);

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                List<AudioTrack> tracks = playlist.getTracks();

                List<Command.Choice> choices = new ArrayList<>();

                int songLimit = 20;

                for (AudioTrack track : tracks) {

                    if (songLimit == 0) {
                        break;
                    }

                    String displayTrack =
                            trimString(track.getInfo().author, 24)
                            + " - " +
                            trimString(track.getInfo().title, 32)
                            + " \u00B7 " +
                            TrackTime.formatClock(track.getDuration());

                    if (!playlist.isSearchResult()) {
                        displayTrack =
                                TrackTitle.format(track, 48)
                                + " \u00B7 " +
                                TrackTime.formatClock(track.getDuration());
                    }

                    if (songLimit == 20) {
                        displayTrack += (parsedQuery.getFlags().toArray().length > 0 ? " \u00B7 " + "Flags: " + parsedQuery.getStringFlags() : "");
                    }

                    String finalDisplayTrack = displayTrack;

                    if (choices.stream().anyMatch(choice -> choice.getName().equals(finalDisplayTrack))) {
                        continue;
                    }

                    choices.add(
                            new Command.Choice(
                                    displayTrack,
                                    (track.getInfo().uri + " " + parsedQuery.getStringFlags())
                            )
                    );

                    songLimit--;

                }

                searchAutoComplete.complete(choices);

            }

            @Override
            public void noMatches() {
                searchAutoComplete.complete(Collections.emptyList());
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                searchAutoComplete.complete(Collections.emptyList());
            }
        });

        return searchAutoComplete.join();

    }

    private String trimString(String string, int length) {

        if (string.length() > length) {
            return string.substring(0, length).trim() + "\u2026";
        }

        return string;

    }

}
