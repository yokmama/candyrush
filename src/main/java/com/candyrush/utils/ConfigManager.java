package com.candyrush.utils;

import com.candyrush.CandyRushPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Utility class for managing configuration values
 */
public class ConfigManager {

    private final CandyRushPlugin plugin;
    private final FileConfiguration config;

    public ConfigManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    // Language settings
    public String getLanguage() {
        return config.getString("language", "ja");
    }

    // Game settings
    public int getMinPlayers() {
        return config.getInt("game.min-players", 2);
    }

    public int getCountdownSeconds() {
        return config.getInt("game.countdown-seconds", 10);
    }

    public int getCooldownMinutes() {
        return config.getInt("game.cooldown-minutes", 5);
    }

    public int getGameDurationMinutes() {
        return config.getInt("game.duration-minutes", 20);
    }

    public int getMapRadius() {
        return config.getInt("game.map-radius", 250);
    }

    public Integer getMapCenterX() {
        if (config.isSet("game.center-x") && config.get("game.center-x") != null) {
            return config.getInt("game.center-x");
        }
        return null;
    }

    public Integer getMapCenterZ() {
        if (config.isSet("game.center-z") && config.get("game.center-z") != null) {
            return config.getInt("game.center-z");
        }
        return null;
    }

    /**
     * マップ中心座標を設定して保存
     */
    public void setMapCenter(int x, int z) {
        config.set("game.center-x", x);
        config.set("game.center-z", z);
        plugin.saveConfig();
    }

    /**
     * マップ中心座標をクリア（nullに設定）
     */
    public void clearMapCenter() {
        config.set("game.center-x", null);
        config.set("game.center-z", null);
        plugin.saveConfig();
    }

    // Treasure settings
    public int getTreasurePerChunk() {
        return config.getInt("treasure.per-chunk", 1);
    }

    public double getTrappedChestDamage() {
        return config.getDouble("treasure.trapped-chest-damage", 4.0);
    }

    public double getTrappedChestEquipmentChance() {
        return config.getDouble("treasure.trapped-chest-equipment-chance", 0.7);
    }

    public int getTreasureRespawnDelay() {
        return config.getInt("treasure.respawn-delay-seconds", 60);
    }

    // Event settings
    public int getEventNpcPerChunks() {
        return config.getInt("event.npc-per-chunks", 3);
    }

    public int getProximityRange() {
        return config.getInt("event.proximity-range", 10);
    }

    public int getHelpMessageCooldown() {
        return config.getInt("event.help-message-cooldown", 5);
    }

    public int getDefenseDurationSeconds() {
        return config.getInt("event.defense-duration-seconds", 120);
    }

    public int getMonsterWaves() {
        return config.getInt("event.monster-waves", 3);
    }

    public int getMonstersPerWave() {
        return config.getInt("event.monsters-per-wave", 5);
    }

    public int getWaveIntervalSeconds() {
        return config.getInt("event.wave-interval-seconds", 30);
    }

    public int getRewardPointsMin() {
        return config.getInt("event.reward-points-min", 50);
    }

    public int getRewardPointsMax() {
        return config.getInt("event.reward-points-max", 100);
    }

    public java.util.List<String> getDefenseMobs() {
        return config.getStringList("mythicmobs.defense-mobs");
    }

    public java.util.List<String> getDefenseEliteMobs() {
        return config.getStringList("mythicmobs.defense-elite-mobs");
    }

    public int getBossSpawnThreshold() {
        return config.getInt("event.boss-spawn-threshold", 3);
    }

    public int getNpcRespawnDelay() {
        return config.getInt("event.npc-respawn-delay-seconds", 120);
    }

    // Murderer settings
    public int getMurdererDurationSeconds() {
        return config.getInt("murderer.duration-seconds", 600);
    }

    // World settings
    public String getWeather() {
        return config.getString("world.weather", "CLEAR");
    }

    public boolean isAutoMorning() {
        return config.getBoolean("world.auto-morning", true);
    }

    // MythicMobs settings
    public String getEventNpcType() {
        return config.getString("mythicmobs.event-npc-type", "EventNPC");
    }

    public java.util.List<String> getBossTypes() {
        java.util.List<String> bossTypes = config.getStringList("mythicmobs.boss-types");
        if (bossTypes.isEmpty()) {
            // Fallback to old config format
            String oldBossType = config.getString("mythicmobs.boss-type", "SugarLord");
            bossTypes = java.util.Arrays.asList(oldBossType);
        }
        return bossTypes;
    }

    // Database settings
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getSqliteFile() {
        return config.getString("database.sqlite.file", "data.db");
    }

    // Debug settings
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean isVerboseLogging() {
        return config.getBoolean("debug.verbose-logging", false);
    }

    // Messages
    public String getPrefix() {
        return config.getString("messages.prefix", "&6[CandyRush] &r");
    }

    /**
     * Reload configuration from disk
     */
    public void reload() {
        plugin.reloadConfig();
    }
}
