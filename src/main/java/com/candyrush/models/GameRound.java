package com.candyrush.models;

/**
 * Represents a completed or ongoing game round (persisted to database)
 */
public class GameRound {

    private final Integer id;  // Null for new rounds not yet saved
    private final long startedAt;  // Epoch timestamp
    private Long endedAt;  // Epoch timestamp, null if ongoing
    private TeamColor winnerTeam;  // Null if no winner yet or tie
    private int totalPlayers;
    private Integer durationSeconds;  // Calculated when game ends
    private final long createdAt;  // Epoch timestamp

    /**
     * Create a new game round (not yet persisted)
     * @param totalPlayers Initial player count
     */
    public GameRound(int totalPlayers) {
        this.id = null;
        long now = System.currentTimeMillis() / 1000;
        this.startedAt = now;
        this.endedAt = null;
        this.winnerTeam = null;
        this.totalPlayers = totalPlayers;
        this.durationSeconds = null;
        this.createdAt = now;
    }

    /**
     * Create a GameRound instance from database values
     */
    public GameRound(Integer id, long startedAt, Long endedAt, TeamColor winnerTeam,
                     int totalPlayers, Integer durationSeconds, long createdAt) {
        this.id = id;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.winnerTeam = winnerTeam;
        this.totalPlayers = totalPlayers;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
    }

    // Getters

    public Integer getId() {
        return id;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public Long getEndedAt() {
        return endedAt;
    }

    public TeamColor getWinnerTeam() {
        return winnerTeam;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // State checks

    /**
     * Check if the game round is currently ongoing
     * @return true if game hasn't ended yet
     */
    public boolean isOngoing() {
        return endedAt == null;
    }

    /**
     * Check if the game round has ended
     * @return true if game has ended
     */
    public boolean hasEnded() {
        return endedAt != null;
    }

    /**
     * Check if there was a winner
     * @return true if a team won
     */
    public boolean hasWinner() {
        return winnerTeam != null;
    }

    /**
     * Check if the game was saved to database
     * @return true if has database ID
     */
    public boolean isPersisted() {
        return id != null;
    }

    // Setters (for ending the game)

    /**
     * End the game round
     * @param winnerTeam Winning team (null for tie/no winner)
     */
    public void endGame(TeamColor winnerTeam) {
        long now = System.currentTimeMillis() / 1000;
        this.endedAt = now;
        this.winnerTeam = winnerTeam;
        this.durationSeconds = (int) (now - startedAt);
    }

    /**
     * Update the total player count
     * @param totalPlayers New player count
     */
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    // Utility methods

    /**
     * Get elapsed time since game start (in seconds)
     * @return Elapsed seconds
     */
    public long getElapsedSeconds() {
        long now = System.currentTimeMillis() / 1000;
        return now - startedAt;
    }

    /**
     * Get remaining time until game end (based on configured duration)
     * @param configuredDuration Game duration in seconds
     * @return Remaining seconds (0 if expired)
     */
    public long getRemainingSeconds(int configuredDuration) {
        long elapsed = getElapsedSeconds();
        long remaining = configuredDuration - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Check if game time has expired
     * @param configuredDuration Game duration in seconds
     * @return true if time is up
     */
    public boolean isTimeExpired(int configuredDuration) {
        return getElapsedSeconds() >= configuredDuration;
    }

    /**
     * Format the game duration as MM:SS
     * @return Formatted duration string
     */
    public String getFormattedDuration() {
        int seconds = durationSeconds != null ? durationSeconds : (int) getElapsedSeconds();
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public String toString() {
        return "GameRound{" +
                "id=" + id +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", winnerTeam=" + winnerTeam +
                ", totalPlayers=" + totalPlayers +
                ", durationSeconds=" + durationSeconds +
                ", ongoing=" + isOngoing() +
                '}';
    }
}
