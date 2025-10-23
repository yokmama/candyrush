package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * プレイヤーが食料を食べた時の処理
 * 自動的にポイントに変換
 */
public class FoodConsumeListener implements Listener {

    private final CandyRushPlugin plugin;

    public FoodConsumeListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material food = event.getItem().getType();

        // ポイント変換処理
        plugin.getPointConversionManager().onPlayerEatFood(player, food);
    }
}
