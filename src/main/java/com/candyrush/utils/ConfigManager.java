package com.candyrush.utils;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.NPCEventTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for managing configuration values
 */
public class ConfigManager {

    private final CandyRushPlugin plugin;
    private final FileConfiguration config;
    private Map<Integer, NPCEventTier> eventTiers;
    private List<String> npcNames;

    public ConfigManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.eventTiers = new HashMap<>();
        this.npcNames = new ArrayList<>();

        // Initialize tier and name configurations
        loadEventTiers();
        loadNpcNames();
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

    public int getTreasureSpawnMinHeight() {
        return config.getInt("treasure.spawn-min-height", 60);
    }

    public int getTreasureSpawnMaxHeight() {
        return config.getInt("treasure.spawn-max-height", 200);
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

    // Tiered NPC Event settings

    /**
     * Get all event tier configurations
     * @return List of all 5 event tiers
     */
    public List<NPCEventTier> getEventTiers() {
        List<NPCEventTier> tiers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            NPCEventTier tier = eventTiers.get(i);
            if (tier != null) {
                tiers.add(tier);
            }
        }
        return tiers;
    }

    /**
     * Get a specific event tier configuration
     * @param tier Tier level (1-5)
     * @return The event tier configuration, or null if not found
     */
    public NPCEventTier getEventTier(int tier) {
        if (tier < 1 || tier > 5) {
            return null;
        }
        return eventTiers.get(tier);
    }

    /**
     * Get the boss spawn point threshold
     * @return Points required to spawn a boss (default 100)
     */
    public int getBossSpawnPointThreshold() {
        return config.getInt("event.boss-spawn-point-threshold", 100);
    }

    /**
     * Get the list of NPC names
     * @return List of NPC names loaded from npc-names.yml
     */
    public List<String> getNpcNames() {
        return new ArrayList<>(npcNames);
    }

    /**
     * Load tier configurations from config.yml
     */
    private void loadEventTiers() {
        eventTiers.clear();

        ConfigurationSection tiersSection = config.getConfigurationSection("event.tiers");
        if (tiersSection == null) {
            plugin.getLogger().warning("No event.tiers section found in config.yml");
            return;
        }

        for (int tier = 1; tier <= 5; tier++) {
            ConfigurationSection tierSection = tiersSection.getConfigurationSection(String.valueOf(tier));
            if (tierSection == null) {
                plugin.getLogger().warning("Tier " + tier + " configuration not found in config.yml");
                continue;
            }

            try {
                String npcMobType = tierSection.getString("npc-mob-type");
                List<String> monsters = tierSection.getStringList("monsters");
                int rewardPointsMin = tierSection.getInt("reward-points-min");
                int rewardPointsMax = tierSection.getInt("reward-points-max");
                int bossSpawnPoints = tierSection.getInt("boss-spawn-points");
                String displayNameFormat = tierSection.getString("display-name-format");

                // Tier-specific difficulty parameters with fallback to global config
                int monsterWaves = tierSection.getInt("monster-waves", config.getInt("event.monster-waves", 3));
                int monstersPerWave = tierSection.getInt("monsters-per-wave", config.getInt("event.monsters-per-wave", 5));
                int defenseDuration = tierSection.getInt("defense-duration-seconds", config.getInt("event.defense-duration-seconds", 120));
                int waveInterval = tierSection.getInt("wave-interval-seconds", config.getInt("event.wave-interval-seconds", 30));

                // Spawn weight for probability (default: equal probability)
                int spawnWeight = tierSection.getInt("spawn-weight", 20);

                // Validate configuration
                if (npcMobType == null || npcMobType.isEmpty()) {
                    plugin.getLogger().severe("Tier " + tier + ": npc-mob-type is missing or empty");
                    continue;
                }
                if (monsters == null || monsters.isEmpty()) {
                    plugin.getLogger().severe("Tier " + tier + ": monsters list is missing or empty");
                    continue;
                }
                if (displayNameFormat == null || !displayNameFormat.contains("{name}")) {
                    plugin.getLogger().severe("Tier " + tier + ": display-name-format must contain {name} placeholder");
                    continue;
                }
                if (rewardPointsMax < rewardPointsMin) {
                    plugin.getLogger().severe("Tier " + tier + ": reward-points-max must be >= reward-points-min");
                    continue;
                }
                if (bossSpawnPoints <= 0) {
                    plugin.getLogger().severe("Tier " + tier + ": boss-spawn-points must be positive");
                    continue;
                }

                NPCEventTier eventTier = new NPCEventTier(
                    tier,
                    npcMobType,
                    monsters,
                    rewardPointsMin,
                    rewardPointsMax,
                    bossSpawnPoints,
                    displayNameFormat,
                    monsterWaves,
                    monstersPerWave,
                    defenseDuration,
                    waveInterval,
                    spawnWeight
                );

                eventTiers.put(tier, eventTier);
                plugin.getLogger().info("Loaded tier " + tier + " configuration: " + eventTier);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load tier " + tier + " configuration: " + e.getMessage());
            }
        }

        if (eventTiers.size() != 5) {
            plugin.getLogger().warning("Expected 5 tiers but loaded " + eventTiers.size() + " tiers");
        }
    }

    /**
     * Load NPC names from npc-names.yml
     */
    private void loadNpcNames() {
        npcNames.clear();

        File namesFile = new File(plugin.getDataFolder(), "npc-names.yml");
        if (!namesFile.exists()) {
            plugin.getLogger().warning("npc-names.yml not found, using default fallback names");
            // Provide fallback names if file doesn't exist
            npcNames.addAll(List.of("太郎", "次郎", "さくら", "花", "悠", "光", "あめちゃん", "キャンディ", "ミント", "ココア"));
            return;
        }

        try {
            YamlConfiguration namesConfig = YamlConfiguration.loadConfiguration(namesFile);
            List<String> names = namesConfig.getStringList("npc-names");

            if (names.isEmpty()) {
                plugin.getLogger().warning("No names found in npc-names.yml, using fallback names");
                npcNames.addAll(List.of("太郎", "次郎", "さくら", "花", "悠", "光", "あめちゃん", "キャンディ", "ミント", "ココア"));
            } else {
                npcNames.addAll(names);
                plugin.getLogger().info("Loaded " + npcNames.size() + " NPC names from npc-names.yml");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load npc-names.yml: " + e.getMessage());
            npcNames.addAll(List.of("太郎", "次郎", "さくら", "花", "悠", "光", "あめちゃん", "キャンディ", "ミント", "ココア"));
        }
    }

    /**
     * Reload configuration from disk
     */
    public void reload() {
        plugin.reloadConfig();
        loadEventTiers();
        loadNpcNames();
    }
}
