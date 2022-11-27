package me.nifty.core.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import me.nifty.core.database.music.PlayerHandler;
import me.nifty.core.database.music.QueueHandler;
import me.nifty.managers.AudioManager;
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

    public void queue(String query, Object event, Member member, List<String> flags) {
        audioManager.loadItem(query, new AudioResultHandler(playerManager, event, member, flags));
    }

    public void skip() {

        int queueSize = queueHandler.getQueueSize();

        int position = playerHandler.getPosition();
        Loop loopMode = playerHandler.getLoopMode();

        int newPosition = position + 1;

        if (queueSize > newPosition) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
            playerHandler.setPosition(newPosition);
        } else if (loopMode == Loop.QUEUE) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(0));
            playerHandler.setPosition(0);
        } else {
            audioPlayer.stopTrack();
        }

    }

    public void back() {

        int position = playerHandler.getPosition();
        Loop loopMode = playerHandler.getLoopMode();

        int newPosition = position - 1;

        if (newPosition >= 0) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(newPosition));
            playerHandler.setPosition(newPosition);
        } else if (loopMode == Loop.QUEUE) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(0));
            playerHandler.setPosition(0);
        } else {
            audioPlayer.stopTrack();
        }

    }

    public void jump(int position) {

        int queueSize = queueHandler.getQueueSize();

        if (position < queueSize) {
            audioPlayer.playTrack(queueHandler.getQueueTrack(position));
            playerHandler.setPosition(position);
        }

    }

    public void pause() {
        audioPlayer.setPaused(true);
    }

    public void unpause() {
        audioPlayer.setPaused(false);
    }

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

    public void remove(int position) {

        int currentPosition = playerHandler.getPosition();

        if (position == currentPosition) {

            int queueSize = queueHandler.getQueueSize();
            Loop loopMode = playerHandler.getLoopMode();

            if (queueSize > currentPosition + 1) {
                audioPlayer.playTrack(queueHandler.getQueueTrack(currentPosition + 1));
            } else if (loopMode == Loop.QUEUE) {
                audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                playerHandler.setPosition(0);
            } else {
                audioPlayer.stopTrack();
            }

        } else if (position < currentPosition) {
            playerHandler.setPosition(currentPosition - 1);
        }

        queueHandler.removeTrack(position);

    }

    public void removeRange(int startPosition, int endPosition) {

        int amount = (endPosition - startPosition) + 1;

        int currentPosition = playerHandler.getPosition();

        if (startPosition <= currentPosition && endPosition >= currentPosition) {

            int queueSize = queueHandler.getQueueSize();
            Loop loopMode = playerHandler.getLoopMode();

            if (queueSize > endPosition + 1) {
                audioPlayer.playTrack(queueHandler.getQueueTrack(endPosition + 1));
                playerHandler.setPosition(startPosition);
            } else if (loopMode == Loop.QUEUE) {
                audioPlayer.playTrack(queueHandler.getQueueTrack(0));
                playerHandler.setPosition(0);
            } else {
                audioPlayer.stopTrack();
            }

        } else if (startPosition < currentPosition) {
            playerHandler.setPosition(currentPosition - amount);
        }

        queueHandler.removeTracks(startPosition, amount);

    }

    public void stop() {
        audioPlayer.stopTrack();
    }

    public void clear() {
        playerHandler.setPosition(0);
        queueHandler.clearQueue();
        audioPlayer.stopTrack();
    }

}
