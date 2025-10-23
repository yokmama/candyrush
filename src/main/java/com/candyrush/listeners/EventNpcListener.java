package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * イベントNPCとのインタラクションを処理するリスナー
 * プレイヤーがNPCをクリックすると防衛イベントが開始される
 */
public class EventNpcListener implements Listener {

    private final CandyRushPlugin plugin;

    public EventNpcListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * プレイヤーがエンティティをクリックしたとき
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // MythicMobsインテグレーションが利用できない場合
        if (plugin.getMythicMobsIntegration() == null) {
            return;
        }

        // MythicMobかチェック
        if (!plugin.getMythicMobsIntegration().isMythicMob(entity)) {
            return;
        }

        // イベントNPCかチェック
        String npcType = plugin.getConfigManager().getEventNpcType();
        plugin.getMythicMobsIntegration().getMobType(entity).ifPresent(mobType -> {
            if (mobType.equals(npcType)) {
                // 防衛イベント開始
                plugin.getEventNpcManager().onPlayerClickNpc(player, entity);
                event.setCancelled(true);
            }
        });
    }
}
