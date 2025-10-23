package com.candyrush.storage;

import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for player data storage operations
 */
public interface PlayerDataStorage {

    /**
     * Load player data by UUID
     * @param uuid Player's UUID
     * @return Optional containing PlayerData if found
     * @throws SQLException if database error occurs
     */
    Optional<PlayerData> loadPlayer(UUID uuid) throws SQLException;

    /**
     * Save or update player data
     * @param playerData Player data to save
     * @throws SQLException if database error occurs
     */
    void savePlayer(PlayerData playerData) throws SQLException;

    /**
     * Delete player data (for GDPR compliance)
     * @param uuid Player's UUID
     * @throws SQLException if database error occurs
     */
    void deletePlayer(UUID uuid) throws SQLException;

    /**
     * Load all players in a specific team
     * @param teamColor Team color
     * @return List of player data
     * @throws SQLException if database error occurs
     */
    List<PlayerData> loadPlayersByTeam(TeamColor teamColor) throws SQLException;

    /**
     * Get top players by points
     * @param limit Maximum number of players to return
     * @return List of top players ordered by points descending
     * @throws SQLException if database error occurs
     */
    List<PlayerData> getTopPlayers(int limit) throws SQLException;

    /**
     * Get all murderers currently active
     * @return List of players with active murderer status
     * @throws SQLException if database error occurs
     */
    List<PlayerData> getActiveMurderers() throws SQLException;

    /**
     * Clear team assignments for all players
     * @throws SQLException if database error occurs
     */
    void clearAllTeamAssignments() throws SQLException;

    /**
     * Reset points for all players
     * @throws SQLException if database error occurs
     */
    void resetAllPoints() throws SQLException;

    /**
     * Get total player count
     * @return Total number of players in database
     * @throws SQLException if database error occurs
     */
    int getTotalPlayerCount() throws SQLException;
}
