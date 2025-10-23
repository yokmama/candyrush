package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * ボスの死亡イベントを処理
 */
public class BossDeathListener implements Listener {

    private final CandyRushPlugin plugin;

    public BossDeathListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();

        // キラーがいない場合は無視
        if (killer == null) {
            return;
        }

        // ボスかチェック
        if (!plugin.getBossManager().isBoss(entity)) {
            return;
        }

        // ボス討伐処理
        plugin.getBossManager().onBossKilled(entity, killer);
    }
}
