package com.candyrush.integration;

import com.candyrush.CandyRushPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

/**
 * Integration with MythicMobs plugin for spawning custom NPCs and bosses
 */
public class MythicMobsIntegration {

    private final CandyRushPlugin plugin;
    private MythicBukkit mythicMobs;
    private boolean initialized = false;

    public MythicMobsIntegration(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize MythicMobs integration
     * @return true if initialization successful
     */
    public boolean initialize() {
        try {
            mythicMobs = MythicBukkit.inst();
            if (mythicMobs == null) {
                plugin.getLogger().severe("MythicMobs instance not found!");
                return false;
            }

            // MythicMobs is loaded if we got the instance
            // No need to check isLoaded() in newer versions

            // Copy MythicMobs configuration files if they don't exist
            copyMythicMobsConfigs();

            initialized = true;
            plugin.getLogger().info("MythicMobs integration initialized successfully");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MythicMobs integration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if MythicMobs integration is ready
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized && mythicMobs != null;
    }

    /**
     * Spawn a MythicMob at a location
     * @param mobType MythicMob internal name (e.g., "EventNPC", "CandyRushBoss")
     * @param location Spawn location
     * @param level Mob level (optional, use 1 for default)
     * @return Optional containing the spawned mob, or empty if spawn failed
     */
    public Optional<ActiveMob> spawnMob(String mobType, Location location, int level) {
        if (!isInitialized()) {
            plugin.getLogger().warning("Attempted to spawn mob but MythicMobs is not initialized");
            return Optional.empty();
        }

        try {
            // Spawn the mob using MythicMobs API
            Object spawnedEntity = mythicMobs.getAPIHelper().spawnMythicMob(mobType, location, level);

            if (spawnedEntity == null) {
                plugin.getLogger().warning("Failed to spawn MythicMob: " + mobType + " (mob type may not exist)");
                return Optional.empty();
            }

            // Convert to ActiveMob
            if (spawnedEntity instanceof org.bukkit.entity.Entity) {
                ActiveMob mob = mythicMobs.getAPIHelper().getMythicMobInstance((org.bukkit.entity.Entity) spawnedEntity);
                if (mob != null) {
                    plugin.getLogger().fine("Spawned MythicMob: " + mobType + " at " + formatLocation(location));
                    return Optional.of(mob);
                }
            }

            plugin.getLogger().warning("Failed to convert spawned mob to ActiveMob: " + mobType);
            return Optional.empty();

        } catch (Exception e) {
            plugin.getLogger().severe("Error spawning MythicMob " + mobType + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Spawn a MythicMob at a location with default level (1)
     * @param mobType MythicMob internal name
     * @param location Spawn location
     * @return Optional containing the spawned mob
     */
    public Optional<ActiveMob> spawnMob(String mobType, Location location) {
        return spawnMob(mobType, location, 1);
    }

    /**
     * Check if an entity is a MythicMob
     * @param entity Entity to check
     * @return true if the entity is a MythicMob
     */
    public boolean isMythicMob(Entity entity) {
        if (!isInitialized()) {
            return false;
        }

        return mythicMobs.getAPIHelper().isMythicMob(entity);
    }

    /**
     * Get the ActiveMob instance for an entity
     * @param entity Bukkit entity
     * @return Optional containing ActiveMob if it's a MythicMob
     */
    public Optional<ActiveMob> getActiveMob(Entity entity) {
        if (!isInitialized()) {
            return Optional.empty();
        }

        try {
            ActiveMob activeMob = mythicMobs.getAPIHelper().getMythicMobInstance(entity);
            return Optional.ofNullable(activeMob);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get the MythicMob type name for an entity
     * @param entity Entity to check
     * @return Optional containing the mob type name
     */
    public Optional<String> getMobType(Entity entity) {
        return getActiveMob(entity).map(mob -> mob.getMobType());
    }

    /**
     * Remove a MythicMob
     * @param activeMob ActiveMob to remove
     */
    public void removeMob(ActiveMob activeMob) {
        if (activeMob != null && activeMob.getEntity() != null) {
            activeMob.getEntity().remove();
        }
    }

    /**
     * Check if a mob type exists in MythicMobs configuration
     * @param mobType Mob type name to check
     * @return true if the mob type exists
     */
    public boolean mobTypeExists(String mobType) {
        if (!isInitialized()) {
            return false;
        }

        return mythicMobs.getMobManager().getMythicMob(mobType).isPresent();
    }

    /**
     * Validate that required mob types exist
     * @return true if all required mobs are configured
     */
    public boolean validateRequiredMobs() {
        if (!isInitialized()) {
            return false;
        }

        String eventNpcType = plugin.getConfigManager().getEventNpcType();
        java.util.List<String> bossTypes = plugin.getConfigManager().getBossTypes();

        boolean valid = true;

        if (!mobTypeExists(eventNpcType)) {
            plugin.getLogger().warning("Event NPC mob type '" + eventNpcType + "' not found in MythicMobs!");
            plugin.getLogger().warning("Please create this mob or update 'mythicmobs.event-npc-type' in config.yml");
            valid = false;
        }

        for (String bossType : bossTypes) {
            if (!mobTypeExists(bossType)) {
                plugin.getLogger().warning("Boss mob type '" + bossType + "' not found in MythicMobs!");
                plugin.getLogger().warning("Please create this mob or update 'mythicmobs.boss-types' in config.yml");
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Copy MythicMobs configuration files from plugin resources
     */
    private void copyMythicMobsConfigs() {
        java.io.File mythicMobsFolder = new java.io.File(plugin.getServer().getPluginManager().getPlugin("MythicMobs").getDataFolder(), "Mobs");
        java.io.File dropTablesFolder = new java.io.File(plugin.getServer().getPluginManager().getPlugin("MythicMobs").getDataFolder(), "DropTables");
        java.io.File skillsFolder = new java.io.File(plugin.getServer().getPluginManager().getPlugin("MythicMobs").getDataFolder(), "Skills");

        if (!mythicMobsFolder.exists()) {
            mythicMobsFolder.mkdirs();
        }
        if (!dropTablesFolder.exists()) {
            dropTablesFolder.mkdirs();
        }
        if (!skillsFolder.exists()) {
            skillsFolder.mkdirs();
        }

        // List of mob configuration files to copy
        String[] mobFiles = {
            "EventNPC.yml",
            "EventNPC_Tier1.yml",
            "EventNPC_Tier2.yml",
            "EventNPC_Tier3.yml",
            "EventNPC_Tier4.yml",
            "EventNPC_Tier5.yml",
            "RaidBoss.yml",
            "TreasureChestBoss.yml",
            "CandyRushBoss.yml",
            "DefenseMobs.yml"
        };

        // Copy mob files
        for (String fileName : mobFiles) {
            java.io.File targetFile = new java.io.File(mythicMobsFolder, fileName);

            // Only copy if file doesn't exist
            if (!targetFile.exists()) {
                try {
                    java.io.InputStream inputStream = plugin.getResource("mythicmobs/mobs/" + fileName);
                    if (inputStream != null) {
                        java.nio.file.Files.copy(inputStream, targetFile.toPath());
                        plugin.getLogger().info("Copied MythicMobs config: " + fileName);
                        inputStream.close();
                    } else {
                        plugin.getLogger().warning("Could not find resource: mythicmobs/mobs/" + fileName);
                    }
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning("Failed to copy MythicMobs config " + fileName + ": " + e.getMessage());
                }
            }
        }

        // Copy drop tables
        String[] dropTableFiles = {
            "CandyRushDrops.yml"
        };

        for (String fileName : dropTableFiles) {
            java.io.File targetFile = new java.io.File(dropTablesFolder, fileName);

            if (!targetFile.exists()) {
                try {
                    java.io.InputStream inputStream = plugin.getResource("mythicmobs/droptables/" + fileName);
                    if (inputStream != null) {
                        java.nio.file.Files.copy(inputStream, targetFile.toPath());
                        plugin.getLogger().info("Copied MythicMobs drop table: " + fileName);
                        inputStream.close();
                    } else {
                        plugin.getLogger().warning("Could not find resource: mythicmobs/droptables/" + fileName);
                    }
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning("Failed to copy MythicMobs drop table " + fileName + ": " + e.getMessage());
                }
            }
        }

        // Copy skills
        String[] skillFiles = {
            "CandyRushSkills.yml"
        };

        for (String fileName : skillFiles) {
            java.io.File targetFile = new java.io.File(skillsFolder, fileName);

            if (!targetFile.exists()) {
                try {
                    java.io.InputStream inputStream = plugin.getResource("mythicmobs/skills/" + fileName);
                    if (inputStream != null) {
                        java.nio.file.Files.copy(inputStream, targetFile.toPath());
                        plugin.getLogger().info("Copied MythicMobs skills: " + fileName);
                        inputStream.close();
                    } else {
                        plugin.getLogger().warning("Could not find resource: mythicmobs/skills/" + fileName);
                    }
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning("Failed to copy MythicMobs skills " + fileName + ": " + e.getMessage());
                }
            }
        }

        // Reload MythicMobs to pick up new configurations
        try {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "mm reload");
            plugin.getLogger().info("Reloaded MythicMobs configuration");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not reload MythicMobs: " + e.getMessage());
        }
    }

    /**
     * Format a location for logging
     * @param loc Location
     * @return Formatted string
     */
    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f in %s",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
    }

    /**
     * Get the MythicMobs API instance
     * @return MythicBukkit instance
     */
    public MythicBukkit getMythicMobs() {
        return mythicMobs;
    }
}
