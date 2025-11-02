package com.candyrush.models;

import java.util.List;
import java.util.Objects;

/**
 * Represents a difficulty tier configuration for NPC events
 * Each tier has different monsters, rewards, boss spawn points, and visual appearance
 */
public class NPCEventTier {

    private final int tier;
    private final String npcMobType;
    private final List<String> monsters;
    private final int rewardPointsMin;
    private final int rewardPointsMax;
    private final int bossSpawnPoints;
    private final String displayNameFormat;
    private final int monsterWaves;
    private final int monstersPerWave;
    private final int defenseDurationSeconds;
    private final int waveIntervalSeconds;
    private final int spawnWeight;

    /**
     * Create a new NPC Event Tier configuration
     *
     * @param tier Tier level (1-5)
     * @param npcMobType MythicMobs mob type ID for the NPC
     * @param monsters List of MythicMobs monster types for defense events
     * @param rewardPointsMin Minimum reward points on completion
     * @param rewardPointsMax Maximum reward points on completion
     * @param bossSpawnPoints Boss spawn points awarded on completion
     * @param displayNameFormat Display name format with {name} placeholder
     * @param monsterWaves Number of monster waves
     * @param monstersPerWave Number of monsters per wave
     * @param defenseDurationSeconds Defense event duration in seconds
     * @param waveIntervalSeconds Interval between waves in seconds
     * @param spawnWeight Spawn probability weight (higher = more common)
     */
    public NPCEventTier(int tier, String npcMobType, List<String> monsters,
                        int rewardPointsMin, int rewardPointsMax,
                        int bossSpawnPoints, String displayNameFormat,
                        int monsterWaves, int monstersPerWave,
                        int defenseDurationSeconds, int waveIntervalSeconds,
                        int spawnWeight) {
        // Validation
        if (tier < 1 || tier > 5) {
            throw new IllegalArgumentException("Tier must be between 1 and 5");
        }
        if (npcMobType == null || npcMobType.isEmpty()) {
            throw new IllegalArgumentException("NPC mob type cannot be null or empty");
        }
        if (monsters == null || monsters.isEmpty()) {
            throw new IllegalArgumentException("Monsters list cannot be null or empty");
        }
        if (rewardPointsMin < 0) {
            throw new IllegalArgumentException("Reward points min cannot be negative");
        }
        if (rewardPointsMax < rewardPointsMin) {
            throw new IllegalArgumentException("Reward points max must be >= min");
        }
        if (bossSpawnPoints <= 0) {
            throw new IllegalArgumentException("Boss spawn points must be positive");
        }
        if (displayNameFormat == null || !displayNameFormat.contains("{name}")) {
            throw new IllegalArgumentException("Display name format must contain {name} placeholder");
        }
        if (monsterWaves <= 0) {
            throw new IllegalArgumentException("Monster waves must be positive");
        }
        if (monstersPerWave <= 0) {
            throw new IllegalArgumentException("Monsters per wave must be positive");
        }
        if (defenseDurationSeconds <= 0) {
            throw new IllegalArgumentException("Defense duration must be positive");
        }
        if (waveIntervalSeconds < 0) {
            throw new IllegalArgumentException("Wave interval cannot be negative");
        }
        if (spawnWeight <= 0) {
            throw new IllegalArgumentException("Spawn weight must be positive");
        }

        this.tier = tier;
        this.npcMobType = npcMobType;
        this.monsters = List.copyOf(monsters); // Immutable copy
        this.rewardPointsMin = rewardPointsMin;
        this.rewardPointsMax = rewardPointsMax;
        this.bossSpawnPoints = bossSpawnPoints;
        this.displayNameFormat = displayNameFormat;
        this.monsterWaves = monsterWaves;
        this.monstersPerWave = monstersPerWave;
        this.defenseDurationSeconds = defenseDurationSeconds;
        this.waveIntervalSeconds = waveIntervalSeconds;
        this.spawnWeight = spawnWeight;
    }

    // Getters

    public int getTier() {
        return tier;
    }

    public String getNpcMobType() {
        return npcMobType;
    }

    public List<String> getMonsters() {
        return monsters;
    }

    public int getRewardPointsMin() {
        return rewardPointsMin;
    }

    public int getRewardPointsMax() {
        return rewardPointsMax;
    }

    public int getBossSpawnPoints() {
        return bossSpawnPoints;
    }

    public String getDisplayNameFormat() {
        return displayNameFormat;
    }

    public int getMonsterWaves() {
        return monsterWaves;
    }

    public int getMonstersPerWave() {
        return monstersPerWave;
    }

    public int getDefenseDurationSeconds() {
        return defenseDurationSeconds;
    }

    public int getWaveIntervalSeconds() {
        return waveIntervalSeconds;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    /**
     * Format a display name by replacing {name} placeholder
     *
     * @param name The name to insert
     * @return Formatted display name
     */
    public String formatDisplayName(String name) {
        return displayNameFormat.replace("{name}", name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NPCEventTier that = (NPCEventTier) o;
        return tier == that.tier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier);
    }

    @Override
    public String toString() {
        return "NPCEventTier{" +
                "tier=" + tier +
                ", npcMobType='" + npcMobType + '\'' +
                ", monsters=" + monsters +
                ", rewardPoints=" + rewardPointsMin + "-" + rewardPointsMax +
                ", bossSpawnPoints=" + bossSpawnPoints +
                '}';
    }
}
