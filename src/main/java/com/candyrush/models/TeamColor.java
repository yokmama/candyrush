package com.candyrush.models;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;

/**
 * Team colors for the 4 fixed teams
 */
public enum TeamColor {
    RED(NamedTextColor.RED, ChatColor.RED, Color.RED, "赤"),
    BLUE(NamedTextColor.BLUE, ChatColor.BLUE, Color.BLUE, "青"),
    GREEN(NamedTextColor.GREEN, ChatColor.GREEN, Color.GREEN, "緑"),
    YELLOW(NamedTextColor.YELLOW, ChatColor.YELLOW, Color.YELLOW, "黄");

    private final NamedTextColor textColor;
    private final ChatColor chatColor;
    private final Color bukkitColor;
    private final String displayName;

    TeamColor(NamedTextColor textColor, ChatColor chatColor, Color bukkitColor, String displayName) {
        this.textColor = textColor;
        this.chatColor = chatColor;
        this.bukkitColor = bukkitColor;
        this.displayName = displayName;
    }

    /**
     * Get the Adventure API text color
     * @return Text color for modern messages
     */
    public NamedTextColor getTextColor() {
        return textColor;
    }

    /**
     * Get the legacy ChatColor
     * @return Chat color for legacy messages
     */
    public ChatColor getChatColor() {
        return chatColor;
    }

    /**
     * Get the Bukkit Color for visual effects
     * @return Bukkit color
     */
    public Color getBukkitColor() {
        return bukkitColor;
    }

    /**
     * Get the Japanese display name
     * @return Display name (e.g., "赤")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the full team name with color
     * @return Formatted team name (e.g., "§c赤チーム")
     */
    public String getFormattedName() {
        return chatColor + displayName + "チーム";
    }
}
