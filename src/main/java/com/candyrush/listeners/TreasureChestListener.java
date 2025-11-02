package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.ChestType;
import com.candyrush.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宝箱の開封イベントを処理
 * トラップチェストのダメージ処理も含む
 */
public class TreasureChestListener implements Listener {

    private final CandyRushPlugin plugin;
    // プレイヤーが現在開いている宝箱の位置を記録
    private final Map<UUID, Location> openChests;

    public TreasureChestListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.openChests = new ConcurrentHashMap<>();
    }

    /**
     * 宝箱が壊されたときの処理
     * アイテムをドロップさせずに宝箱だけを削除する
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // 宝箱タイプをチェック
        ChestType chestType = getChestType(block.getType());
        if (chestType == null) {
            return;
        }

        // ゲーム生成チェストかどうかをチェック
        if (!plugin.getTreasureChestManager().isGameChest(block.getLocation())) {
            // プレイヤーが設置したチェストなので処理しない（通常通り壊せる）
            return;
        }

        Player player = event.getPlayer();

        // アイテムドロップを完全に防ぐ
        event.setDropItems(false);

        // インベントリの中身を削除（念のため）
        if (block.getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) block.getState();
            Inventory inventory = holder.getInventory();
            inventory.clear();
        }

        // ブロックを削除（通常通り破壊を許可）
        // event.setCancelled(false); はデフォルト

        // エフェクトを表示
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 0.3, 0.3, 0.3, 0.05);
        block.getWorld().playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);

        // プレイヤーに通知
        MessageUtils.sendActionBar(player, "&7宝箱を壊した - 中身は消えた...");

        // マネージャーに通知（リスポーン処理のため）
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getTreasureChestManager().onChestOpened(block.getLocation());
        });

        plugin.getLogger().fine("Player " + player.getName() + " broke chest at " +
                              block.getX() + "," + block.getY() + "," + block.getZ() +
                              " - items not dropped");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // 宝箱タイプをチェック
        ChestType chestType = getChestType(block.getType());
        if (chestType == null) {
            return;
        }

        // ゲーム生成チェストかどうかをチェック
        if (!plugin.getTreasureChestManager().isGameChest(block.getLocation())) {
            // プレイヤーが設置したチェストなので処理しない
            return;
        }

        // トラップチェストの場合、ダメージを与える
        if (chestType == ChestType.TRAPPED_CHEST) {
            handleTrappedChest(player, block);
        }

        // 宝箱の位置を記録
        Location chestLocation = block.getLocation();
        if (!openChests.containsKey(player.getUniqueId()) ||
            !openChests.get(player.getUniqueId()).equals(chestLocation)) {
            // 新しい宝箱を開いた - 統計をインクリメント
            plugin.getPlayerManager().getPlayerData(player.getUniqueId()).ifPresent(data -> {
                data.incrementChestsOpened();
                plugin.getPlayerManager().savePlayerData(data);
            });
        }
        openChests.put(player.getUniqueId(), chestLocation);

        // 宝箱を開くことを許可（イベントはキャンセルしない）
        // プレイヤーは通常通りインベントリを開ける
    }

    /**
     * インベントリのアイテムがクリックされたとき
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        // 宝箱のインベントリでない場合は無視
        if (clickedInventory == null || clickedInventory.getLocation() == null) {
            return;
        }

        Location chestLocation = clickedInventory.getLocation();

        // ゲーム生成チェストかどうかをチェック
        if (!plugin.getTreasureChestManager().isGameChest(chestLocation)) {
            // プレイヤーが設置したチェストなので処理しない
            return;
        }

        Block block = chestLocation.getBlock();

        // 宝箱タイプをチェック
        ChestType chestType = getChestType(block.getType());
        if (chestType == null) {
            return;
        }

        // クリックされたアイテムを取得
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 食べ物の場合、ポイントに変換してインベントリには入れない
        if (isFoodItem(clickedItem.getType())) {
            event.setCancelled(true); // イベントをキャンセル

            // ポイントを計算
            int points = calculateFoodPoints(clickedItem);

            // ポイントを加算（個人とチーム）
            plugin.getPlayerManager().addPoints(player.getUniqueId(), points);

            // チームポイントも加算
            plugin.getPlayerManager().getPlayerData(player.getUniqueId()).ifPresent(data -> {
                if (data.getTeamColor() != null) {
                    plugin.getTeamManager().addTeamPoints(data.getTeamColor(), points);
                }
            });

            // メッセージ表示
            MessageUtils.sendActionBar(player, "&e&l+" + points + "pt &7" + clickedItem.getType().name() + " x" + clickedItem.getAmount());

            // サウンド再生
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

            // アイテムを削除
            clickedInventory.setItem(event.getSlot(), null);

            plugin.getLogger().fine("Player " + player.getName() + " got " + points + " points from " +
                                  clickedItem.getType().name() + " x" + clickedItem.getAmount());
        }

        // 1tick後に空かチェック（アイテム移動後にチェック）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            checkAndCloseIfEmpty(player, chestLocation);
        }, 1L);
    }

    /**
     * インベントリでアイテムがドラッグされたとき
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory inventory = event.getInventory();

        // 宝箱のインベントリでない場合は無視
        if (inventory.getLocation() == null) {
            return;
        }

        Location chestLocation = inventory.getLocation();

        // ゲーム生成チェストかどうかをチェック
        if (!plugin.getTreasureChestManager().isGameChest(chestLocation)) {
            // プレイヤーが設置したチェストなので処理しない
            return;
        }

        Block block = chestLocation.getBlock();

        // 宝箱タイプをチェック
        ChestType chestType = getChestType(block.getType());
        if (chestType == null) {
            return;
        }

        // ドラッグしているアイテムが食べ物の場合はキャンセル
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && isFoodItem(draggedItem.getType())) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            MessageUtils.sendActionBar(player, "&cお菓子はクリックして取得してください");
        }
    }

    /**
     * 宝箱が空になったら自動的に閉じて削除
     */
    private void checkAndCloseIfEmpty(Player player, Location chestLocation) {
        Block block = chestLocation.getBlock();

        if (!(block.getState() instanceof InventoryHolder)) {
            return;
        }

        InventoryHolder holder = (InventoryHolder) block.getState();
        Inventory inventory = holder.getInventory();

        // 宝箱が空かチェック
        if (isChestEmpty(inventory)) {
            // プレイヤーのインベントリを強制的に閉じる
            player.closeInventory();

            // 少し遅延させて閉じたあとに削除演出
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                deleteEmptyChest(block, player);
            }, 5L);

            // 記録から削除
            openChests.remove(player.getUniqueId());
        }
    }

    /**
     * インベントリが閉じられたとき（記録をクリーンアップ）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // 記録から削除
        openChests.remove(player.getUniqueId());
    }

    /**
     * 宝箱が空かチェック
     */
    private boolean isChestEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    /**
     * 空の宝箱を煙とともに削除
     */
    private void deleteEmptyChest(Block block, Player player) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        // 煙のパーティクル効果
        block.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.3, 0.3, 0.3, 0.1);
        block.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 0.2, 0.2, 0.2, 0.05);

        // サウンド効果
        block.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        block.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.8f, 1.2f);

        // 宝箱を削除
        block.setType(Material.AIR);

        // プレイヤーに通知
        MessageUtils.sendActionBar(player, "&7宝箱が消えた...");

        // マネージャーに通知（リスポーン処理）
        plugin.getTreasureChestManager().onChestOpened(block.getLocation());

        plugin.getLogger().fine("Player " + player.getName() + " emptied chest at " +
                              block.getX() + "," + block.getY() + "," + block.getZ() +
                              " - chest removed");
    }

    /**
     * トラップチェストの処理
     */
    private void handleTrappedChest(Player player, Block block) {
        double damage = plugin.getConfigManager().getTrappedChestDamage();

        // ダメージを与える
        player.damage(damage);

        // メッセージ表示
        MessageUtils.sendMessage(player, "&c&lトラップだ！ ダメージを受けた！");
        MessageUtils.sendActionBar(player, "&c&l⚠ トラップチェスト ⚠");

        plugin.getLogger().fine("Player " + player.getName() + " triggered a trapped chest");
    }

    /**
     * アイテムが食べ物かチェック
     */
    private boolean isFoodItem(Material material) {
        switch (material) {
            // お菓子系
            case COOKIE:
            case CAKE:
            case PUMPKIN_PIE:
            case GOLDEN_APPLE:
            case ENCHANTED_GOLDEN_APPLE:
            case SWEET_BERRIES:
            case GLOW_BERRIES:
            case GOLDEN_CARROT:
            // 通常の食料
            case APPLE:
            case MELON_SLICE:
            case BREAD:
            case COOKED_BEEF:
            case COOKED_CHICKEN:
            case COOKED_MUTTON:
            case COOKED_PORKCHOP:
            case COOKED_SALMON:
            case COOKED_COD:
            case BAKED_POTATO:
            case CARROT:
            case POTATO:
            case BEETROOT:
            case DRIED_KELP:
            case TROPICAL_FISH:
            case PUFFERFISH:
            case BEEF:
            case CHICKEN:
            case MUTTON:
            case PORKCHOP:
            case RABBIT:
            case SALMON:
            case COD:
                return true;
            default:
                return false;
        }
    }

    /**
     * 食べ物アイテムからポイントを計算
     */
    private int calculateFoodPoints(ItemStack item) {
        int basePoints = 1; // 基本ポイント
        int amount = item.getAmount();

        // お菓子系は高ポイント
        switch (item.getType()) {
            case COOKIE:
                basePoints = 2;
                break;
            case CAKE:
            case PUMPKIN_PIE:
                basePoints = 5;
                break;
            case GOLDEN_APPLE:
                basePoints = 10;
                break;
            case ENCHANTED_GOLDEN_APPLE:
                basePoints = 20;
                break;
            case SWEET_BERRIES:
            case GLOW_BERRIES:
                basePoints = 3;
                break;
            case GOLDEN_CARROT:
                basePoints = 8;
                break;
            default:
                basePoints = 1; // その他の食べ物
                break;
        }

        return basePoints * amount;
    }

    /**
     * ブロックタイプから宝箱タイプを取得
     */
    private ChestType getChestType(Material material) {
        for (ChestType type : ChestType.values()) {
            if (type.getMaterial() == material) {
                return type;
            }
        }
        return null;
    }
}
