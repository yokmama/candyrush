package com.candyrush.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Utility class for formatting and sending messages
 */
public class MessageUtils {

    /**
     * Translate color codes in a string (&a -> green, etc.)
     * @param message Message with & color codes
     * @return Translated message
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Create a Component from a colored string
     * @param message Message with & color codes
     * @return Component
     */
    public static Component toComponent(String message) {
        return Component.text(colorize(message));
    }

    /**
     * Send a colored chat message to a player
     * @param player Target player
     * @param message Message with & color codes
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    /**
     * Send a title to a player
     * @param player Target player
     * @param title Title text
     * @param subtitle Subtitle text
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        Title titleObject = Title.title(
            toComponent(title),
            toComponent(subtitle),
            Title.Times.times(
                Duration.ofMillis(500),  // fade in
                Duration.ofSeconds(3),   // stay
                Duration.ofMillis(500)   // fade out
            )
        );
        player.showTitle(titleObject);
    }

    /**
     * Send a title to a player with custom timing
     * @param player Target player
     * @param title Title text
     * @param subtitle Subtitle text
     * @param fadeIn Fade in duration (ticks)
     * @param stay Stay duration (ticks)
     * @param fadeOut Fade out duration (ticks)
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        Title titleObject = Title.title(
            toComponent(title),
            toComponent(subtitle),
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)
            )
        );
        player.showTitle(titleObject);
    }

    /**
     * Send an action bar message to a player
     * @param player Target player
     * @param message Message text
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(toComponent(message));
    }

    /**
     * Format a countdown message
     * @param seconds Remaining seconds
     * @return Formatted countdown message
     */
    public static String formatCountdown(int seconds) {
        if (seconds <= 3) {
            return "&c&l" + seconds;
        } else if (seconds <= 5) {
            return "&e&l" + seconds;
        } else {
            return "&a" + seconds;
        }
    }

    /**
     * Format time in MM:SS format
     * @param seconds Total seconds
     * @return Formatted time string
     */
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * Format points with commas
     * @param points Point value
     * @return Formatted string (e.g., "1,234")
     */
    public static String formatPoints(int points) {
        return String.format("%,d", points);
    }

    /**
     * Create a success message component
     * @param message Message text
     * @return Success styled component
     */
    public static Component success(String message) {
        return Component.text(message)
            .color(NamedTextColor.GREEN);
    }

    /**
     * Create an error message component
     * @param message Message text
     * @return Error styled component
     */
    public static Component error(String message) {
        return Component.text(message)
            .color(NamedTextColor.RED);
    }

    /**
     * Create a warning message component
     * @param message Message text
     * @return Warning styled component
     */
    public static Component warning(String message) {
        return Component.text(message)
            .color(NamedTextColor.YELLOW);
    }

    /**
     * Create a highlight message component
     * @param message Message text
     * @return Highlighted component
     */
    public static Component highlight(String message) {
        return Component.text(message)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true);
    }
}
