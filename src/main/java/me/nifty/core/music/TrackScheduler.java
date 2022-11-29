package me.nifty.core.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.core.music.handlers.AudioResultHandler;
import me.nifty.managers.AudioManager;
import me.nifty.utils.enums.Autoplay;
import me.nifty.utils.enums.Loop;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

public class TrackScheduler {

    private final AudioPlayerManager audioManager = AudioManager.getAudioManager();

    private final PlayerManager playerManager;

    private final AudioPlayer audioPlayer;

    private final PlayerHandler playerHandler;
    private final QueueHandler queueHandler;

    public TrackScheduler(PlayerManager playerManager) {
        this.playerManager = playerManager;

        this.audioPlayer = playerManager.getAudioPlayer();

        this.playerHandler = playerManager.getPlayerHandler();
        this.queueHandler = playerManager.getQueueHandler();
    }

    /**
     * Adds the queried track(s) to the queue.
     *
     * @param query The query to add to the queue.
     * @param event The event that triggered the command and that will be used to reply to the user.
     * @param member The member that triggered the command.
     * @param flags Optional flags to add the track with.
     * @see AudioResultHandler
     */
    public void queue(String query, Object event, Member member, List<String> flags) {
        audioManager.loadItem(query, new AudioResultHandler(playerManager, event, member, flags));
    }

    /**
     * Skips the current track from the queue.
     */
    public void skip() {

        audioPlayer.stopTrack();

        int queueSize = queueHandler.getQueueSize();

        int position = playerHandler.getPosition();

        Autoplay autoplayMode = playerHandler.getAutoplayMode();
        Loop loopMode = playerHandler.getLoopMode();

        int newPosition = position + 1;

        if (queueSize > newPosition) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
            playerHandler.setPosition(newPosition);
        } else if (loopMode == Loop.QUEUE) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(0));
            playerHandler.setPosition(0);
        } else if (autoplayMode == Autoplay.ENABLED) {
            playerManager.getAutoplayManager().autoplay();
        }

    }

    /**
     * Goes back to the previous track in the queue.
     */
    public void back() {

        audioPlayer.stopTrack();

        int position = playerHandler.getPosition();
        Loop loopMode = playerHandler.getLoopMode();

        int newPosition = position - 1;

        if (newPosition >= 0) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
            playerHandler.setPosition(newPosition);
        } else if (loopMode == Loop.QUEUE) {
            int queueSize = queueHandler.getQueueSize();

            audioPlayer.playTrack(queueHandler.getQueueTrack(queueSize - 1));
            playerHandler.setPosition(queueSize - 1);
        }

    }

    /**
     * Jumps to the specified position in the queue.
     *
     * @param position The position to jump to.
     */
    public void jump(int position) {

        int queueSize = queueHandler.getQueueSize();

        if (position < queueSize) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(position));
            playerHandler.setPosition(position);
        }

    }

    /**
     * Pauses the player.
     */
    public void pause() {
        audioPlayer.setPaused(true);
    }

    /**
     * Unpauses the player.
     */
    public void unpause() {
        audioPlayer.setPaused(false);
    }

    /**
     * Moves the specified track to the specified position in the queue.
     *
     * @param position The position to move the track to.
     * @param newPosition The position of the track to move.
     */
    public void move(int position, int newPosition) {

        int currentPosition = playerHandler.getPosition();

        if (position == currentPosition) {
            playerHandler.setPosition(newPosition);
        } else if (position < currentPosition && newPosition >= currentPosition) {
            playerHandler.setPosition(currentPosition - 1);
        } else if (position > currentPosition && newPosition <= currentPosition) {
            playerHandler.setPosition(currentPosition + 1);
        }

        queueHandler.moveTrack(position, newPosition);

    }

    /**
     * Moves the specified range of tracks to the specified position in the queue.
     *
     * @param startPosition The start position of the range.
     * @param endPosition The end position of the range.
     * @param newPosition The position to move the range to.
     */
    public void moveRange(int startPosition, int endPosition, int newPosition) {

        int amount = (endPosition - startPosition) + 1;
        int offset = newPosition - startPosition;

        int currentPosition = playerHandler.getPosition();

        if (startPosition <= currentPosition && endPosition >= currentPosition) {

            if (newPosition < startPosition) {
                playerHandler.setPosition(currentPosition + offset);
            } else if (newPosition > endPosition) {
                playerHandler.setPosition(currentPosition + ((offset - amount) + 1));
            }

        } else if (startPosition > currentPosition && newPosition <= currentPosition) {
            playerHandler.setPosition(currentPosition + amount);
        } else if (startPosition < currentPosition && newPosition >= currentPosition) {
            playerHandler.setPosition(currentPosition - amount);
        }

        queueHandler.moveTracks(startPosition, newPosition, amount);

    }

    /**
     * Removes the specified track from the queue.
     *
     * @param position The position of the track to remove.
     */
    public void remove(int position) {

        // Gets the current position of the player and the queue size before removing the track.
        int currentPosition = playerHandler.getPosition();
        int queueSize = queueHandler.getQueueSize();

        // Removes the track from the queue.
        queueHandler.removeTrack(position);

        // If the track that was removed was the current track, skip to the next track.
        if (position == currentPosition) {

            // Stops the current track.
            audioPlayer.stopTrack();

            // Gets the player settings.
            Autoplay autoplayMode = playerHandler.getAutoplayMode();
            Loop loopMode = playerHandler.getLoopMode();

            if (queueSize > currentPosition + 1) {

                // If the queue has a track after the current track, play it.
                audioPlayer.playTrack(queueHandler.getQueueTrack(currentPosition));
                playerHandler.setPosition(currentPosition);

            } else if (loopMode == Loop.QUEUE) {

                // If the queue is looping and there is no next track, play the first track in the queue.
                audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                playerHandler.setPosition(0);

            } else if (autoplayMode == Autoplay.ENABLED) {

                // If autoplay is enabled and there is no next track, play the autoplay track.
                playerManager.getAutoplayManager().autoplay();

            }

        } else if (position < currentPosition) {
            // If the track that was removed was before the current track, decrease the current position by 1.
            playerHandler.setPosition(currentPosition - 1);
        }

    }

    /**
     * Removes the specified range of tracks from the queue.
     *
     * @param startPosition The start position of the range.
     * @param endPosition The end position of the range.
     */
    public void removeRange(int startPosition, int endPosition) {

        // Gets the amount of tracks that are being removed.
        int amount = (endPosition - startPosition) + 1;

        // Gets the current position of the player and the queue size before removing the tracks.
        int currentPosition = playerHandler.getPosition();
        int queueSize = queueHandler.getQueueSize();

        // Removes the tracks from the queue.
        queueHandler.removeTracks(startPosition, amount);

        // If the range that was removed contains the current track, skip to the next track.
        if (startPosition <= currentPosition && endPosition >= currentPosition) {

            // Stops the current track.
            audioPlayer.stopTrack();

            // Gets the player settings.
            Autoplay autoplayMode = playerHandler.getAutoplayMode();
            Loop loopMode = playerHandler.getLoopMode();

            if (queueSize > endPosition + 1) {

                // If the queue has a track after the end of the range, play it.
                audioPlayer.playTrack(queueHandler.getQueueTrack(startPosition));
                playerHandler.setPosition(startPosition);

            } else if (loopMode == Loop.QUEUE) {

                // If the queue is looping and there is no next track, play the first track in the queue.
                audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                playerHandler.setPosition(0);

            } else if (autoplayMode == Autoplay.ENABLED) {

                // If autoplay is enabled and there is no next track, play the autoplay track.
                playerManager.getAutoplayManager().autoplay();

            } else {

                // If there is no next track, update the current position to the last one.
                int newQueueSize = queueHandler.getQueueSize();
                playerHandler.setPosition(newQueueSize - 1);

            }

        } else if (startPosition < currentPosition) {
            // If the range that was removed was before the current track, decrease the current position by the amount of tracks removed.
            playerHandler.setPosition(currentPosition - amount);
        }

    }

    /**
     * Stops the current track.
     */
    public void stop() {
        audioPlayer.stopTrack();
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        playerHandler.setPosition(0);
        queueHandler.clearQueue();
        audioPlayer.stopTrack();
    }

}
