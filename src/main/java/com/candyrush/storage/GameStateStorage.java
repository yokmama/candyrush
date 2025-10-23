package com.candyrush.storage;

import com.candyrush.models.GameRound;
import com.candyrush.models.TeamColor;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for game state and round history storage
 */
public interface GameStateStorage {

    /**
     * Create a new game round in the database
     * @param gameRound Game round to create
     * @return The round ID assigned by database
     * @throws SQLException if database error occurs
     */
    int createGameRound(GameRound gameRound) throws SQLException;

    /**
     * Update an existing game round (typically when ending the game)
     * @param gameRound Game round to update
     * @throws SQLException if database error occurs
     */
    void updateGameRound(GameRound gameRound) throws SQLException;

    /**
     * Load a game round by ID
     * @param roundId Round ID
     * @return Optional containing GameRound if found
     * @throws SQLException if database error occurs
     */
    Optional<GameRound> loadGameRound(int roundId) throws SQLException;

    /**
     * Get the most recent game round
     * @return Optional containing the latest GameRound
     * @throws SQLException if database error occurs
     */
    Optional<GameRound> getLatestGameRound() throws SQLException;

    /**
     * Get recent game rounds
     * @param limit Maximum number of rounds to return
     * @return List of recent rounds ordered by start time descending
     * @throws SQLException if database error occurs
     */
    List<GameRound> getRecentGameRounds(int limit) throws SQLException;

    /**
     * Save team scores for a specific round
     * @param roundId Round ID
     * @param teamColor Team color
     * @param points Final points
     * @param kills Total kills
     * @param deaths Total deaths
     * @param playersCount Number of players in team
     * @throws SQLException if database error occurs
     */
    void saveTeamScore(int roundId, TeamColor teamColor, int points, int kills, int deaths, int playersCount) throws SQLException;

    /**
     * Load team scores for a specific round
     * @param roundId Round ID
     * @return Map of team color to their scores (points, kills, deaths, players)
     * @throws SQLException if database error occurs
     */
    Map<TeamColor, TeamScore> loadTeamScores(int roundId) throws SQLException;

    /**
     * Save player stats for a specific round
     * @param roundId Round ID
     * @param playerUuid Player's UUID
     * @param teamColor Player's team color
     * @param pointsEarned Points earned this round
     * @param kills Kills this round
     * @param deaths Deaths this round
     * @param chestsOpened Chests opened this round
     * @param foodDeposited Food deposited this round
     * @param becameMurderer Whether player became murderer
     * @throws SQLException if database error occurs
     */
    void savePlayerStats(int roundId, String playerUuid, TeamColor teamColor, int pointsEarned,
                        int kills, int deaths, int chestsOpened, int foodDeposited, boolean becameMurderer) throws SQLException;

    /**
     * Get total games played
     * @return Total number of game rounds
     * @throws SQLException if database error occurs
     */
    int getTotalGamesPlayed() throws SQLException;

    /**
     * Get win counts by team
     * @return Map of team color to number of wins
     * @throws SQLException if database error occurs
     */
    Map<TeamColor, Integer> getTeamWinCounts() throws SQLException;

    /**
     * Data class for team score information
     */
    class TeamScore {
        public final int points;
        public final int kills;
        public final int deaths;
        public final int playersCount;

        public TeamScore(int points, int kills, int deaths, int playersCount) {
            this.points = points;
            this.kills = kills;
            this.deaths = deaths;
            this.playersCount = playersCount;
        }
    }
}
