package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.MessageUtils;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ボス戦の管理
 * プレイヤーが3回Murdererになるとボスが出現
 */
public class BossManager {

    private final CandyRushPlugin plugin;
    private final Map<UUID, Integer> murdererCounts; // プレイヤーUUID -> Murderer回数
    private final Set<UUID> activeBosses; // 現在アクティブなボスのUUID
    private final Map<UUID, UUID> bossOwners; // ボスUUID -> 召喚したプレイヤーUUID
    private final Map<UUID, org.bukkit.boss.BossBar> bossBars; // ボスUUID -> BossBar
    private int maxActiveBosses = 1; // 同時に存在できるボスの最大数

    public BossManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.murdererCounts = new ConcurrentHashMap<>();
        this.activeBosses = ConcurrentHashMap.newKeySet();
        this.bossOwners = new ConcurrentHashMap<>();
        this.bossBars = new ConcurrentHashMap<>();
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        murdererCounts.clear();
        plugin.getLogger().info("BossManager initialized");
    }

    /**
     * プレイヤーがMurdererになった時の処理
     * （現在はカウントのみ、自動召喚は無効）
     */
    public void onPlayerBecomeMurderer(UUID playerUuid) {
        int count = murdererCounts.getOrDefault(playerUuid, 0) + 1;
        murdererCounts.put(playerUuid, count);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            plugin.getLogger().info("Player " + player.getName() + " became murderer " +
                                  count + " times");
        }
    }

    /**
     * ボスを召喚
     */
    public void spawnBoss(Player player) {
        // MythicMobsが利用できない場合はスキップ
        if (plugin.getMythicMobsIntegration() == null) {
            plugin.getLogger().warning("Cannot spawn boss - MythicMobs not available");
            MessageUtils.sendMessage(player, "&cボスを召喚できませんでした（MythicMobs未インストール）");
            return;
        }

        // ボスタイプをランダムに選択
        List<String> bossTypes = plugin.getConfigManager().getBossTypes();
        if (bossTypes.isEmpty()) {
            plugin.getLogger().warning("No boss types configured!");
            return;
        }
        String bossType = bossTypes.get(new Random().nextInt(bossTypes.size()));
        Location spawnLoc = player.getLocation().add(0, 0, 5); // プレイヤーの前方5ブロック

        plugin.getMythicMobsIntegration().spawnMob(bossType, spawnLoc, 1).ifPresent(activeMob -> {
            Entity bossEntity = activeMob.getEntity().getBukkitEntity();
            activeBosses.add(bossEntity.getUniqueId());
            bossOwners.put(bossEntity.getUniqueId(), player.getUniqueId());

            // 全プレイヤーに通知
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&c&l⚠⚠⚠ ボス出現！ ⚠⚠⚠"));
            Bukkit.broadcastMessage(MessageUtils.colorize(
                "&e" + player.getName() + " &cが強大なボスを召喚しました！"));

            // サウンド再生
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
                MessageUtils.sendTitle(p, "&4&lボス出現！", "&c討伐するとポイント獲得");
            }

            plugin.getLogger().info("Boss spawned for player: " + player.getName() +
                                  " at " + formatLocation(spawnLoc));
        });
    }

    /**
     * 指定位置にボスを召喚（レイドイベント用）
     */
    public void spawnBoss(Location location) {
        // 既に最大数のボスが存在する場合はスポーンしない
        if (activeBosses.size() >= maxActiveBosses) {
            plugin.getLogger().warning("Cannot spawn boss - maximum active bosses reached (" + maxActiveBosses + ")");
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&c既にボスが存在するため、新しいボスは出現しません"));
            return;
        }

        // ボスタイプをランダムに選択
        List<String> bossTypes = plugin.getConfigManager().getBossTypes();
        if (bossTypes.isEmpty()) {
            plugin.getLogger().warning("No boss types configured!");
            return;
        }
        String bossType = bossTypes.get(new Random().nextInt(bossTypes.size()));
        plugin.getLogger().info("Selected boss type: " + bossType);

        plugin.getMythicMobsIntegration().spawnMob(bossType, location, 1).ifPresent(activeMob -> {
            Entity bossEntity = activeMob.getEntity().getBukkitEntity();
            UUID bossUuid = bossEntity.getUniqueId();
            activeBosses.add(bossUuid);

            // ボスバーを作成
            createBossBar(bossEntity);

            // 全プレイヤーに通知
            Bukkit.broadcastMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() +
                "&6&l★ &d&lお菓子の王 シュガーロード &6&l出現！ ★"));

            // サウンド再生
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }

            plugin.getLogger().info("Boss spawned at " + formatLocation(location) + " - UUID: " + bossUuid);
        });
    }

    /**
     * ボスが倒された時の処理
     */
    public void onBossKilled(Entity boss, Player killer) {
        UUID bossUuid = boss.getUniqueId();

        if (!activeBosses.contains(bossUuid)) {
            return; // このボスは管理下にない
        }

        activeBosses.remove(bossUuid);
        UUID ownerUuid = bossOwners.remove(bossUuid);

        // ボスバーを削除
        removeBossBar(bossUuid);

        // キラーにポイント付与
        int bossPoints = 500; // ボス討伐ボーナス
        PlayerData killerData = plugin.getPlayerManager().getOrCreatePlayerData(killer);
        killerData.addPoints(bossPoints);
        plugin.getPlayerManager().savePlayerData(killerData);

        // チームにもポイント付与
        TeamColor teamColor = killerData.getTeamColor();
        if (teamColor != null) {
            plugin.getTeamManager().addTeamPoints(teamColor, bossPoints);
        }

        // 全プレイヤーに通知
        Bukkit.broadcastMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() +
            "&a&l✦ ボス討伐成功！ ✦"));
        Bukkit.broadcastMessage(MessageUtils.colorize(
            "&e" + killer.getName() + " &aがボスを倒しました！"));
        Bukkit.broadcastMessage(MessageUtils.colorize(
            "&6+" + bossPoints + "pt 獲得！"));

        // サウンド再生
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // キラーへの特別メッセージ
        MessageUtils.sendTitle(killer,
            "&6&l+500pt",
            "&aボス討伐ボーナス！");

        plugin.getLogger().info("Boss killed by " + killer.getName() +
                              " - awarded " + bossPoints + " points");
    }

    /**
     * ボスがアクティブかチェック
     */
    public boolean isBoss(Entity entity) {
        return activeBosses.contains(entity.getUniqueId());
    }

    /**
     * Murdererカウントをリセット
     */
    public void resetMurdererCounts() {
        murdererCounts.clear();
        plugin.getLogger().info("Murderer counts reset");
    }

    /**
     * すべてのボスを削除
     */
    public void removeAllBosses() {
        // 全ボスバーを削除
        for (UUID bossUuid : new HashSet<>(bossBars.keySet())) {
            removeBossBar(bossUuid);
        }

        activeBosses.clear();
        bossOwners.clear();
        murdererCounts.clear();
        plugin.getLogger().info("All bosses removed");
    }

    /**
     * ボスバーを作成
     */
    private void createBossBar(Entity bossEntity) {
        if (!(bossEntity instanceof org.bukkit.entity.LivingEntity)) {
            return;
        }

        org.bukkit.entity.LivingEntity livingBoss = (org.bukkit.entity.LivingEntity) bossEntity;
        UUID bossUuid = bossEntity.getUniqueId();

        // ボスバーを作成
        org.bukkit.boss.BossBar bossBar = Bukkit.createBossBar(
            MessageUtils.colorize("&6&l★ &d&lお菓子の王 シュガーロード &6&l★"),
            org.bukkit.boss.BarColor.PINK,
            org.bukkit.boss.BarStyle.SEGMENTED_10
        );

        // 全プレイヤーに表示
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        // 初期HP設定
        double healthPercentage = livingBoss.getHealth() / livingBoss.getMaxHealth();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, healthPercentage)));

        bossBars.put(bossUuid, bossBar);

        // HP更新タスクを開始
        startBossHealthUpdateTask(bossUuid, livingBoss, bossBar);

        plugin.getLogger().info("Boss bar created for boss: " + bossUuid);
    }

    /**
     * ボスのHP更新タスクを開始
     */
    private void startBossHealthUpdateTask(UUID bossUuid, org.bukkit.entity.LivingEntity boss, org.bukkit.boss.BossBar bossBar) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                // ボスが死んでいるか存在しない場合
                if (!boss.isValid() || boss.isDead() || !activeBosses.contains(bossUuid)) {
                    removeBossBar(bossUuid);
                    return;
                }

                // HP更新
                double healthPercentage = boss.getHealth() / boss.getMaxHealth();
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, healthPercentage)));

                // 新しく参加したプレイヤーにも表示
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                }
            }
        }, 0L, 10L); // 0.5秒ごとに更新
    }

    /**
     * ボスバーを削除
     */
    private void removeBossBar(UUID bossUuid) {
        org.bukkit.boss.BossBar bossBar = bossBars.remove(bossUuid);
        if (bossBar != null) {
            bossBar.removeAll();
            plugin.getLogger().info("Boss bar removed for boss: " + bossUuid);
        }
    }

    /**
     * 座標フォーマット
     */
    private String formatLocation(Location loc) {
        return String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * クリーンアップ
     */
    public void shutdown() {
        removeAllBosses();
        plugin.getLogger().info("BossManager shutdown complete");
    }
}

