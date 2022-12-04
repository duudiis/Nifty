package me.nifty.utils.formatting;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.List;

public class SearchResultSelectMenu {

    public static SelectMenu get(List<AudioTrack> searchTracks, long memberId, List<String> flags) {

        StringSelectMenu.Builder selectMenu = StringSelectMenu.create("search_" + memberId + "_" + String.join(",", flags))
            .setPlaceholder("Make a selection")
            .setRequiredRange(1, Math.min(searchTracks.size(), 25));

        for (int i = 0; i < Math.min(searchTracks.size(), 25); i++) {

            selectMenu.addOption(
                    TrackTitle.format(searchTracks.get(i), 80),
                    searchTracks.get(i).getInfo().uri,
                    (searchTracks.get(i).getInfo().author + " \u00B7 " + TrackTime.formatClock(searchTracks.get(i).getDuration()))
            );

        }

        return selectMenu.build();

    }

}
