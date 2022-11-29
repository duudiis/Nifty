package me.nifty.core.music.managers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.nifty.core.music.PlayerManager;
import me.nifty.managers.AudioManager;
import me.nifty.utils.formatting.ErrorEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AutoplayManager {

    private static final int maxPageLimit = 10;
    private static final int maxAttempts = 3;

    private final AudioPlayerManager audioManager = AudioManager.getAudioManager();
    private final PlayerManager playerManager;

    private final List<String> excludedTracks = new ArrayList<>();

    public AutoplayManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void autoplay() {

        int queueSize = playerManager.getQueueHandler().getQueueSize();

        AudioTrack seedTrack = getSeedTrack();
        AudioTrack autoplayTrack = getAutoplayTrack(seedTrack, 0);

        if (autoplayTrack == null && queueSize != 0) {
            autoplayError();
            return;
        }

        playerManager.getQueueHandler().addTrack(autoplayTrack, queueSize);

        playerManager.getAudioPlayer().playTrack(autoplayTrack);
        playerManager.getPlayerHandler().setPosition(queueSize);

    }

    public void autoplayError() {

        TextChannel textChannel = playerManager.getGuild().getTextChannelById(playerManager.getPlayerHandler().getTextChannelId());
        if (textChannel == null) { return; }

        try {
            textChannel.sendMessageEmbeds(ErrorEmbed.get("Could not AutoPlay from the previous track!")).queue();
        } catch (Exception ignored) {}

    }

    /**
     * Gets a track from the queue that meets the requirements for autoplaying.
     *
     * @return The seed track.
     */
    private AudioTrack getSeedTrack() {

        AudioTrack seedTrack = null;

        int queueSize = playerManager.getQueueHandler().getQueueSize();

        int searchPage = (queueSize % 10 == 0) ? ((queueSize / 10) - 1) : (queueSize / 10);
        int searchPageLimit = Math.max(0, searchPage - maxPageLimit);

        while (seedTrack == null && searchPage >= searchPageLimit) {

            List<AudioTrack> latestTracks = playerManager.getQueueHandler().getQueuePage(searchPage);

            for (AudioTrack track : latestTracks) {

                if (!track.getSourceManager().getSourceName().equals("youtube")) {
                    continue;
                }

                if (excludedTracks.contains(track.getIdentifier())) {
                    continue;
                }

                if (track.getInfo().isStream) {
                    continue;
                }

                seedTrack = track;
            }

            searchPage--;

        }

        return seedTrack;

    }

    /**
     * Gets a related track from the seed track.
     *
     * @param seedTrack The track to search for related tracks
     * @param attempts The number of attempts already made to find a related track
     * @return The related track
     */
    private AudioTrack getAutoplayTrack(AudioTrack seedTrack, int attempts) {

        if (seedTrack == null) {
            return null;
        }

        String seedTrackId = seedTrack.getInfo().identifier;

        String radioId = "RD" + seedTrackId;
        String radioUrl = "https://www.youtube.com/watch?v=" + seedTrackId + "&list=" + radioId;

        CompletableFuture<AudioTrack> futureAutoplayTrack = new CompletableFuture<>();

        audioManager.loadItem(radioUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                futureAutoplayTrack.complete(null);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                List<AudioTrack> autoplayTracks = removeQueueDuplicates(playlist.getTracks());

                AudioTrack autoplayTrack = autoplayTracks.get((int) (Math.random() * autoplayTracks.size()));
                autoplayTrack.setUserData(playerManager.getGuild().getSelfMember().getUser().getIdLong());

                futureAutoplayTrack.complete(autoplayTrack);

            }

            @Override
            public void noMatches() {
                futureAutoplayTrack.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                futureAutoplayTrack.complete(null);
            }
        });

        AudioTrack autoplayTrack = futureAutoplayTrack.join();

        if (autoplayTrack == null && attempts < maxAttempts) {
            excludedTracks.add(seedTrackId);
            AudioTrack nextSeedTrack = getSeedTrack();
            return getAutoplayTrack(nextSeedTrack, attempts + 1);
        }

        return autoplayTrack;

    }

    /**
     * Removes tracks from the autoplay list that are already in the queue.
     *
     * @param tracks The list of tracks to remove duplicates from.
     * @return The list of tracks with duplicates removed.
     */
    private List<AudioTrack> removeQueueDuplicates(List<AudioTrack> tracks) {

        int queueSize = playerManager.getQueueHandler().getQueueSize();

        int searchPage = (queueSize % 10 == 0) ? ((queueSize / 10) - 1) : (queueSize / 10);
        int searchPageLimit = Math.max(0, searchPage - maxPageLimit);

        while (searchPage >= searchPageLimit) {

            List<AudioTrack> latestTracks = playerManager.getQueueHandler().getQueuePage(searchPage);

            for (AudioTrack track : latestTracks) {

                tracks.removeIf(t -> t.getInfo().identifier.equals(track.getInfo().identifier));

            }

            searchPage--;

        }

        return tracks;

    }

}
