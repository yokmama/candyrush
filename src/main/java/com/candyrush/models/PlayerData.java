package com.candyrush.models;

import java.util.UUID;

/**
 * Represents a player's persistent data in the game
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private TeamColor teamColor;
    private int points;
    private int kills;
    private int deaths;
    private boolean isMurderer;
    private long murdererUntil;  // Epoch timestamp
    private long lastSeen;  // Epoch timestamp
    private final long createdAt;  // Epoch timestamp
    private long updatedAt;  // Epoch timestamp

    /**
     * Create a new PlayerData instance for a new player
     * @param uuid Player's UUID
     * @param name Player's name
     */
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.teamColor = null;  // Not assigned to a team yet
        this.points = 0;
        this.kills = 0;
        this.deaths = 0;
        this.isMurderer = false;
        this.murdererUntil = 0;
        long now = System.currentTimeMillis() / 1000;
        this.lastSeen = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Create a PlayerData instance from database values
     */
    public PlayerData(UUID uuid, String name, TeamColor teamColor, int points, int kills, int deaths,
                      boolean isMurderer, long murdererUntil, long lastSeen, long createdAt, long updatedAt) {
        this.uuid = uuid;
        this.name = name;
        this.teamColor = teamColor;
        this.points = points;
        this.kills = kills;
        this.deaths = deaths;
        this.isMurderer = isMurderer;
        this.murdererUntil = murdererUntil;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public TeamColor getTeamColor() {
        return teamColor;
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

    public boolean isMurderer() {
        return isMurderer;
    }

    public long getMurdererUntil() {
        return murdererUntil;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // Setters (update timestamp on modification)

    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setPoints(int points) {
        this.points = points;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void addPoints(int amount) {
        this.points += amount;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setKills(int kills) {
        this.kills = kills;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void incrementKills() {
        this.kills++;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void incrementDeaths() {
        this.deaths++;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setMurderer(boolean isMurderer) {
        this.isMurderer = isMurderer;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setMurdererUntil(long murdererUntil) {
        this.murdererUntil = murdererUntil;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Update last seen to current time
     */
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis() / 1000;
        this.updatedAt = this.lastSeen;
    }

    /**
     * Check if the player is currently a murderer (based on timestamp)
     * @return true if murderer status is active
     */
    public boolean isMurdererActive() {
        if (!isMurderer) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        return now < murdererUntil;
    }

    /**
     * Clear murderer status
     */
    public void clearMurderer() {
        this.isMurderer = false;
        this.murdererUntil = 0;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Reset player data for a new game (keeps total stats)
     */
    public void resetForNewGame() {
        this.teamColor = null;
        this.points = 0;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Get K/D ratio
     * @return Kill/Death ratio (0 if no deaths)
     */
    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", teamColor=" + teamColor +
                ", points=" + points +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", isMurderer=" + isMurderer +
                '}';
    }
}
