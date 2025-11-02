package com.candyrush.tasks;

import com.candyrush.CandyRushPlugin;
import com.candyrush.managers.PlayerManager;
import com.candyrush.models.PlayerData;
import com.candyrush.storage.PlayerDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Scheduled task to check and expire murderer status for online players
 * Runs every 20 seconds (400 ticks)
 */
public class MurdererExpirationTask extends BukkitRunnable {

    private final CandyRushPlugin plugin;
    private final PlayerManager playerManager;
    private final PlayerDataStorage playerDataStorage;

    public MurdererExpirationTask(CandyRushPlugin plugin,
                                  PlayerManager playerManager,
                                  PlayerDataStorage playerDataStorage) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.playerDataStorage = playerDataStorage;
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
        int expiredCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerData data = playerManager.getPlayerData(player.getUniqueId()).orElse(null);

                if (data == null) {
                    continue; // Player data not loaded yet
                }

                // Check if player is marked as murderer with an expiration time
                if (data.isMurderer() && data.getMurdererUntil() > 0) {
                    // Check if expiration time has passed
                    if (currentTime > data.getMurdererUntil()) {
                        // Clear murderer status
                        data.clearMurderer();

                        // Save to database asynchronously
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                playerDataStorage.savePlayer(data);
                            } catch (SQLException e) {
                                plugin.getLogger().log(Level.SEVERE,
                                    "Failed to save player data after murderer expiration for " + player.getName(), e);
                            }
                        });

                        // Update player's team color (back to normal team)
                        playerManager.updatePlayerTeamColor(player);

                        // Notify player
                        player.sendMessage("§a殺人者ステータスが解除されました");

                        // Log event
                        plugin.getLogger().info("Expired murderer status for " + player.getName() +
                            " (was set until " + data.getMurdererUntil() + ", now is " + currentTime + ")");

                        expiredCount++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "Error checking murderer expiration for " + player.getName(), e);
                // Continue processing other players
            }
        }

        if (expiredCount > 0) {
            plugin.getLogger().info("MurdererExpirationTask: Expired " + expiredCount + " murderer statuses");
        }
    }
}
