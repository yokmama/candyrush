package com.candyrush.models;

import org.bukkit.Material;

/**
 * Types of treasure chests that can spawn in the game
 * Total: 11 types (10 normal + 1 trap)
 */
public enum ChestType {
    CHEST(Material.CHEST, false),
    LARGE_CHEST(Material.CHEST, false),  // Will spawn 2 adjacent chests
    BARREL(Material.BARREL, false),
    FURNACE(Material.FURNACE, false),
    BLAST_FURNACE(Material.BLAST_FURNACE, false),
    SMOKER(Material.SMOKER, false),
    BREWING_STAND(Material.BREWING_STAND, false),
    HOPPER(Material.HOPPER, false),
    DROPPER(Material.DROPPER, false),
    DISPENSER(Material.DISPENSER, false),
    TRAPPED_CHEST(Material.TRAPPED_CHEST, true);  // Special: damage + equipment

    private final Material material;
    private final boolean isTrapped;

    ChestType(Material material, boolean isTrapped) {
        this.material = material;
        this.isTrapped = isTrapped;
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
