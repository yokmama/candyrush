package com.candyrush.models;

/**
 * Game lifecycle states
 */
public enum GameState {
    /**
     * Waiting for minimum players to join
     */
    WAITING,

    /**
     * Countdown before game starts (10 seconds)
     */
    COUNTDOWN,

    /**
     * Game is actively running
     */
    RUNNING,

    /**
     * Cooldown period after game ends (5 minutes)
     */
    COOLDOWN;

    /**
     * Check if the game is in a playable state
     * @return true if game is running
     */
    public boolean isPlaying() {
        return this == RUNNING;
    }

    /**
     * Check if players can join the game
     * @return true if in waiting or countdown state
     */
    public boolean canJoin() {
        return this == WAITING || this == COUNTDOWN;
    }

    /**
     * Check if the game can start
     * @return true if in waiting state
     */
    public boolean canStart() {
        return this == WAITING;
    }
}
