package com.candyrush.models;

import org.bukkit.Location;

import java.util.*;

/**
 * Represents a team in the current game session
 * This is runtime data (not persisted to database)
 */
public class Team {

    private final TeamColor color;
    private final Set<UUID> playerUuids;
    private int points;
    private int kills;
    private int deaths;
    private Location spawnPoint;
    private Location flagLocation;  // Team flag/base location

    /**
     * Create a new team
     * @param color Team color
     */
    public Team(TeamColor color) {
        this.color = color;
        this.playerUuids = new HashSet<>();
        this.points = 0;
        this.kills = 0;
        this.deaths = 0;
        this.spawnPoint = null;
        this.flagLocation = null;
    }

    // Getters

    public TeamColor getColor() {
        return color;
    }

    public Set<UUID> getPlayerUuids() {
        return Collections.unmodifiableSet(playerUuids);
    }

    public int getPlayerCount() {
        return playerUuids.size();
    }

    public int getPoints() {
        return points;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public Location getFlagLocation() {
        return flagLocation;
    }

    // Player management

    /**
     * Add a player to the team
     * @param uuid Player's UUID
     * @return true if player was added (wasn't already in team)
     */
    public boolean addPlayer(UUID uuid) {
        return playerUuids.add(uuid);
    }

    /**
     * Remove a player from the team
     * @param uuid Player's UUID
     * @return true if player was removed (was in team)
     */
    public boolean removePlayer(UUID uuid) {
        return playerUuids.remove(uuid);
    }

    /**
     * Check if a player is in this team
     * @param uuid Player's UUID
     * @return true if player is in team
     */
    public boolean hasPlayer(UUID uuid) {
        return playerUuids.contains(uuid);
    }

    /**
     * Clear all players from the team
     */
    public void clearPlayers() {
        playerUuids.clear();
    }

    // Score management

    /**
     * Add points to the team
     * @param amount Points to add (can be negative)
     */
    public void addPoints(int amount) {
        this.points += amount;
        if (this.points < 0) {
            this.points = 0;
        }
    }

    /**
     * Set the team's points
     * @param points New points value
     */
    public void setPoints(int points) {
        this.points = Math.max(0, points);
    }

    /**
     * Increment kill count
     */
    public void incrementKills() {
        this.kills++;
    }

    /**
     * Increment death count
     */
    public void incrementDeaths() {
        this.deaths++;
    }

    /**
     * Add kills to the team
     * @param amount Kills to add
     */
    public void addKills(int amount) {
        this.kills += Math.max(0, amount);
    }

    /**
     * Add deaths to the team
     * @param amount Deaths to add
     */
    public void addDeaths(int amount) {
        this.deaths += Math.max(0, amount);
    }

    // Location management

    /**
     * Set the team's spawn point
     * @param location Spawn location
     */
    public void setSpawnPoint(Location location) {
        this.spawnPoint = location != null ? location.clone() : null;
    }

    /**
     * Set the team's flag/base location
     * @param location Flag location
     */
    public void setFlagLocation(Location location) {
        this.flagLocation = location != null ? location.clone() : null;
    }

    // Utility methods

    /**
     * Reset the team for a new game (keep spawn points)
     */
    public void reset() {
        this.points = 0;
        this.kills = 0;
        this.deaths = 0;
        this.playerUuids.clear();
        // Keep spawn point and flag location
    }

    /**
     * Get the team's K/D ratio
     * @return Kill/Death ratio (0 if no deaths)
     */
    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    /**
     * Get average points per player
     * @return Average points (0 if no players)
     */
    public double getAveragePoints() {
        if (playerUuids.isEmpty()) {
            return 0;
        }
        return (double) points / playerUuids.size();
    }

    /**
     * Check if the team is empty
     * @return true if no players in team
     */
    public boolean isEmpty() {
        return playerUuids.isEmpty();
    }

    /**
     * Get formatted team name with color
     * @return Colored team name (e.g., "§c赤チーム")
     */
    public String getFormattedName() {
        return color.getFormattedName();
    }

    @Override
    public String toString() {
        return "Team{" +
                "color=" + color +
                ", players=" + playerUuids.size() +
                ", points=" + points +
                ", kills=" + kills +
                ", deaths=" + deaths +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return color == team.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }
}
