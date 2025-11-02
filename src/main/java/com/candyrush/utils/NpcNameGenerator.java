package com.candyrush.utils;

import com.candyrush.models.NPCEventTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating random NPC names from a configured pool
 * Thread-safe and provides formatted names with tier prefixes
 */
public class NpcNameGenerator {

    private final List<String> names;
    private final Random random;

    /**
     * Create a new NpcNameGenerator with the given name pool
     *
     * @param names List of available NPC names (must not be empty)
     * @throws IllegalArgumentException if names is null or empty
     */
    public NpcNameGenerator(List<String> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("Name pool cannot be null or empty");
        }
        // Create immutable copy for thread safety
        this.names = new ArrayList<>(names);
        this.random = new Random();
    }

    /**
     * Get a random name from the pool
     *
     * @return A randomly selected name
     */
    public String getRandomName() {
        return names.get(random.nextInt(names.size()));
    }

    /**
     * Get a formatted name with tier display format
     *
     * @param tier The NPCEventTier to use for formatting
     * @return Formatted name with tier prefix (e.g., "&a[Lv.1] Satoshi")
     */
    public String getFormattedName(NPCEventTier tier) {
        String randomName = getRandomName();
        return tier.formatDisplayName(randomName);
    }

    /**
     * Get the total number of available names in the pool
     *
     * @return The size of the name pool
     */
    public int getNamePoolSize() {
        return names.size();
    }

    /**
     * Check if the name pool is adequately sized
     *
     * @param minimumSize Minimum recommended size
     * @return true if pool size meets minimum
     */
    public boolean hasAdequateNames(int minimumSize) {
        return names.size() >= minimumSize;
    }
}
