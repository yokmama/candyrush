package com.candyrush.models;

import org.bukkit.Material;

/**
 * Types of treasure chests that can spawn in the game
 * Total: 11 types (10 normal + 1 trap)
 */
public enum ChestType {
    CHEST(Material.CHEST, false, ChestLootCategory.FOOD),
    LARGE_CHEST(Material.CHEST, false, ChestLootCategory.FOOD),  // Will spawn 2 adjacent chests
    BARREL(Material.BARREL, false, ChestLootCategory.FOOD),
    FURNACE(Material.FURNACE, false, ChestLootCategory.MATERIAL),
    BLAST_FURNACE(Material.BLAST_FURNACE, false, ChestLootCategory.MATERIAL),
    SMOKER(Material.SMOKER, false, ChestLootCategory.MATERIAL),
    BREWING_STAND(Material.BREWING_STAND, false, ChestLootCategory.POTION),
    HOPPER(Material.HOPPER, false, ChestLootCategory.UTILITY),
    DROPPER(Material.DROPPER, false, ChestLootCategory.EQUIPMENT),
    DISPENSER(Material.DISPENSER, false, ChestLootCategory.EQUIPMENT),
    TRAPPED_CHEST(Material.TRAPPED_CHEST, true, ChestLootCategory.TRAP_REWARD);  // Special: damage + high-tier equipment

    private final Material material;
    private final boolean isTrapped;
    private final ChestLootCategory lootCategory;

    ChestType(Material material, boolean isTrapped, ChestLootCategory lootCategory) {
        this.material = material;
        this.isTrapped = isTrapped;
        this.lootCategory = lootCategory;
    }

    /**
     * Get the Bukkit material for this chest type
     * @return Material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Check if this is a trapped chest
     * @return true if trapped (deals damage, gives equipment)
     */
    public boolean isTrapped() {
        return isTrapped;
    }

    /**
     * Get the loot category for this chest type
     * @return ChestLootCategory
     */
    public ChestLootCategory getLootCategory() {
        return lootCategory;
    }

    /**
     * Check if this is a container that can hold items
     * @return true if it's a container
     */
    public boolean isContainer() {
        return material.toString().contains("CHEST") ||
               material.toString().contains("BARREL") ||
               material.toString().contains("FURNACE") ||
               material.toString().contains("SMOKER") ||
               material.toString().contains("BREWING_STAND") ||
               material.toString().contains("HOPPER") ||
               material.toString().contains("DROPPER") ||
               material.toString().contains("DISPENSER");
    }

    /**
     * Get a random chest type with weighted probability
     * CHEST: 70%, Others: 27%, TRAPPED_CHEST: 3%
     * @return Random ChestType
     */
    public static ChestType random() {
        double rand = Math.random() * 100;

        // 70% chance for regular CHEST
        if (rand < 70) {
            return CHEST;
        }

        // 3% chance for TRAPPED_CHEST
        if (rand < 73) {
            return TRAPPED_CHEST;
        }

        // 27% chance for other types
        ChestType[] others = {
            LARGE_CHEST, BARREL, FURNACE, BLAST_FURNACE,
            SMOKER, BREWING_STAND, HOPPER, DROPPER, DISPENSER
        };
        return others[(int) (Math.random() * others.length)];
    }

    /**
     * Get a random normal (non-trapped) chest type
     * @return Random non-trapped ChestType
     */
    public static ChestType randomNormal() {
        ChestType type;
        do {
            type = random();
        } while (type.isTrapped());
        return type;
    }
}
