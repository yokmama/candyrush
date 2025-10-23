package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.GameState;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 金塊・金インゴットを右クリックでポイントに変換
 */
public class GoldItemConvertListener implements Listener {

    private final CandyRushPlugin plugin;

    // 変換レート
    private static final int GOLD_NUGGET_POINTS = 1;  // 金塊 = 1pt
    private static final int GOLD_INGOT_POINTS = 9;   // 金インゴット = 9pt (金塊9個分)

    public GoldItemConvertListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 右クリックのみ
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // ゲーム中のみ変換可能
        if (plugin.getGameManager().getCurrentState() != GameState.RUNNING) {
            return;
        }

        // 金塊または金インゴットかチェック
        Material material = item.getType();
        int pointsPerItem = 0;

        if (material == Material.GOLD_NUGGET) {
            pointsPerItem = GOLD_NUGGET_POINTS;
        } else if (material == Material.GOLD_INGOT) {
            pointsPerItem = GOLD_INGOT_POINTS;
        } else {
            return; // 金アイテムでない場合は処理しない
        }

        // イベントをキャンセル（ブロック設置などを防ぐ）
        event.setCancelled(true);

        // アイテムの数量を取得
        int amount = item.getAmount();
        int totalPoints = pointsPerItem * amount;

        // ポイントを付与
        plugin.getPlayerManager().addPoints(player.getUniqueId(), totalPoints);

        // アイテムを削除
        player.getInventory().setItemInMainHand(null);

        // 通知
        String itemName = material == Material.GOLD_NUGGET ? "金塊" : "金インゴット";
        MessageUtils.sendMessage(player, "&6&l" + itemName + " x" + amount + " &7を &a&l+" + totalPoints + "pt &7に変換しました！");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        plugin.getLogger().info("Player " + player.getName() + " converted " + amount + " " + material + " to " + totalPoints + " points");
    }
}
