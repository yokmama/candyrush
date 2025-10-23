package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvPとMurderer処理を担当
 */
public class PvpListener implements Listener {

    private final CandyRushPlugin plugin;
    private final Map<UUID, UUID> lastVictims; // Murderer UUID -> 最後に攻撃した被害者のUUID

    public PvpListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.lastVictims = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Murdererかチェック
        boolean victimIsMurderer = plugin.getPlayerManager().isMurderer(victim.getUniqueId());

        // Murdererでない場合はアイテムを保持
        if (!victimIsMurderer) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        } else {
            // Murdererの場合はアイテムをドロップ
            plugin.getLogger().info("Murderer " + victim.getName() + " died - items will drop");
        }

        // PvPでない場合は無視
        if (killer == null) {
            handleNonPvpDeath(victim);
            return;
        }

        // 自殺の場合は無視
        if (killer.equals(victim)) {
            handleNonPvpDeath(victim);
            return;
        }

        // PvP処理
        handlePvpKill(killer, victim);
    }

    /**
     * PvP以外の死亡処理
     */
    private void handleNonPvpDeath(Player victim) {
        PlayerData victimData = plugin.getPlayerManager().getOrCreatePlayerData(victim);
        TeamColor victimTeam = victimData.getTeamColor();

        // デスカウント
        victimData.incrementDeaths();
        plugin.getPlayerManager().savePlayerData(victimData);

        if (victimTeam != null) {
            plugin.getTeamManager().incrementTeamDeaths(victimTeam);
        }
    }

    /**
     * PvPキル処理
     */
    private void handlePvpKill(Player killer, Player victim) {
        PlayerData killerData = plugin.getPlayerManager().getOrCreatePlayerData(killer);
        PlayerData victimData = plugin.getPlayerManager().getOrCreatePlayerData(victim);

        TeamColor killerTeam = killerData.getTeamColor();
        TeamColor victimTeam = victimData.getTeamColor();

        // 同じチームかチェック
        if (killerTeam != null && killerTeam.equals(victimTeam)) {
            // 被害者がMurdererの場合はペナルティなし
            if (plugin.getPlayerManager().isMurderer(victim.getUniqueId())) {
                Bukkit.broadcastMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() +
                    "&e" + killer.getName() + " &fが Murderer &c" + victim.getName() + " &fを倒しました！"));
                plugin.getLogger().info(killer.getName() + " killed Murderer " + victim.getName() + " (no penalty)");
                return;
            }

            // チームキル - Murderer化
            applyMurdererPenalty(killer, victim, killerData);
            return;
        }

        // 通常のPvPキル
        killerData.incrementKills();
        victimData.incrementDeaths();

        plugin.getPlayerManager().savePlayerData(killerData);
        plugin.getPlayerManager().savePlayerData(victimData);

        if (killerTeam != null) {
            plugin.getTeamManager().incrementTeamKills(killerTeam);
        }
        if (victimTeam != null) {
            plugin.getTeamManager().incrementTeamDeaths(victimTeam);
        }

        // メッセージ表示
        String killerTeamName = killerTeam != null ? killerTeam.getFormattedName() : "&7無所属";
        String victimTeamName = victimTeam != null ? victimTeam.getFormattedName() : "&7無所属";

        Bukkit.broadcastMessage(MessageUtils.colorize(
            killerTeamName + " &f" + killer.getName() + " &7> " +
            victimTeamName + " &f" + victim.getName()));

        plugin.getLogger().info("PvP: " + killer.getName() + " killed " + victim.getName());
    }

    /**
     * Murdererペナルティを適用
     */
    private void applyMurdererPenalty(Player killer, Player victim, PlayerData killerData) {
        int durationSeconds = 3 * 60; // 3分（180秒）

        // Murderer状態に設定（累積、最大60分）
        // 戻り値: 初めてMurdererになった場合true
        boolean isFirstTime = plugin.getPlayerManager().setMurderer(killer.getUniqueId(), durationSeconds);

        // 前回の被害者と比較
        UUID lastVictimUuid = lastVictims.get(killer.getUniqueId());
        boolean isDifferentVictim = lastVictimUuid == null || !lastVictimUuid.equals(victim.getUniqueId());

        // 今回の被害者を記録
        lastVictims.put(killer.getUniqueId(), victim.getUniqueId());

        // アナウンス条件: 初回 OR 異なるプレイヤーへの攻撃
        boolean shouldAnnounce = isFirstTime || isDifferentVictim;

        if (isFirstTime) {
            // 初回のみ実行

            // 名前を赤色に変更
            killer.setDisplayName("§c" + killer.getName());
            killer.setPlayerListName("§c" + killer.getName());

            // 防具を剥奪
            removeArmor(killer);

            // ボスマネージャーに通知（3回でボス召喚）
            plugin.getBossManager().onPlayerBecomeMurderer(killer.getUniqueId());

            // 全プレイヤーに通知（誰が誰を攻撃したか）
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&c&l" + killer.getName() + " &fが &e" + victim.getName() + " &fを攻撃しました！"));
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&c&l" + killer.getName() + " がチームキルを行い、Murdererになりました！"));
            Bukkit.broadcastMessage(MessageUtils.colorize(
                "&e防具が剥奪され、最大60分間装備できません"));

            // キラーへのメッセージ
            MessageUtils.sendTitle(killer,
                "&4&lMURDERER",
                "&cチームキルごとに+3分 (最大60分)");

            plugin.getLogger().warning("Player " + killer.getName() + " became a murderer for the first time (team kill on " + victim.getName() + ")");
        } else if (shouldAnnounce) {
            // 2回目以降でも、異なるプレイヤーへの攻撃時はアナウンス
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&c&l" + killer.getName() + " &fが &e" + victim.getName() + " &fを攻撃しました！"));

            plugin.getLogger().info("Player " + killer.getName() + " team killed " + victim.getName() + " (different victim, murderer time extended by 3 minutes)");
        } else {
            // 同じプレイヤーへの攻撃 - アナウンスなし、時間のみ延長
            plugin.getLogger().info("Player " + killer.getName() + " team killed " + victim.getName() + " again (murderer time extended by 3 minutes)");
        }
    }

    /**
     * プレイヤーの防具をインベントリに移動
     */
    private void removeArmor(Player player) {
        // ヘルメット
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            player.getInventory().addItem(helmet);
            player.getInventory().setHelmet(null);
        }

        // チェストプレート
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            player.getInventory().addItem(chestplate);
            player.getInventory().setChestplate(null);
        }

        // レギンス
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR) {
            player.getInventory().addItem(leggings);
            player.getInventory().setLeggings(null);
        }

        // ブーツ
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            player.getInventory().addItem(boots);
            player.getInventory().setBoots(null);
        }
    }

    /**
     * プレイヤーリスポーン時にショップアイテムを再付与
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // 少し遅延させてショップアイテムを付与（リスポーン処理が完了した後）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getShopManager().giveShopItem(player);
        }, 5L); // 0.25秒後
    }

    /**
     * Murdererの防具装備を防ぐリスナー（インベントリクリック）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorEquip(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Murdererかチェック
        if (!plugin.getPlayerManager().isMurderer(player.getUniqueId())) {
            return;
        }

        // 防具スロット（5-8）へのクリックを防ぐ
        int slot = event.getSlot();
        if (slot >= 36 && slot <= 39) { // 防具スロット
            event.setCancelled(true);
            MessageUtils.sendActionBar(player, "&c&lMurderer状態では防具を装備できません！");
            return;
        }

        // クリックしたアイテムが防具かチェック（Shift+クリック対策）
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() != Material.AIR && isArmor(item.getType())) {
            // Shift+クリックの場合
            if (event.isShiftClick()) {
                event.setCancelled(true);
                MessageUtils.sendActionBar(player, "&c&lMurderer状態では防具を装備できません！");
            }
        }
    }

    /**
     * Murdererの防具装備を防ぐリスナー（右クリック装備）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorEquipInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Murdererかチェック
        if (!plugin.getPlayerManager().isMurderer(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() != Material.AIR && isArmor(item.getType())) {
            event.setCancelled(true);
            MessageUtils.sendActionBar(player, "&c&lMurderer状態では防具を装備できません！");
        }
    }

    /**
     * アイテムが防具かチェック
     */
    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") ||
               name.endsWith("_BOOTS");
    }
}
