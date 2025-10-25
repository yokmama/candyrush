package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * プレイヤーデータを管理するマネージャー
 * オンラインプレイヤーのキャッシュとデータベース連携を担当
 */
public class PlayerManager {

    private final CandyRushPlugin plugin;
    private final Map<UUID, PlayerData> playerDataCache;

    public PlayerManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        plugin.getLogger().info("PlayerManager initialized");
    }

    /**
     * プレイヤーデータを取得（キャッシュまたはDB）
     */
    public Optional<PlayerData> getPlayerData(UUID uuid) {
        // キャッシュから取得
        PlayerData cached = playerDataCache.get(uuid);
        if (cached != null) {
            return Optional.of(cached);
        }

        // データベースから読み込み
        try {
            Optional<PlayerData> loaded = plugin.getPlayerDataStorage().loadPlayer(uuid);
            loaded.ifPresent(data -> playerDataCache.put(uuid, data));
            return loaded;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            return Optional.empty();
        }
    }

    /**
     * プレイヤーデータを取得または新規作成
     */
    public PlayerData getOrCreatePlayerData(Player player) {
        return getPlayerData(player.getUniqueId())
                .orElseGet(() -> createNewPlayerData(player));
    }

    /**
     * 新規プレイヤーデータを作成
     */
    private PlayerData createNewPlayerData(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId(), player.getName());
        playerDataCache.put(player.getUniqueId(), data);

        // データベースに保存
        try {
            plugin.getPlayerDataStorage().savePlayer(data);
            plugin.getLogger().info("Created new player data for: " + player.getName());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save new player data", e);
        }

        return data;
    }

    /**
     * プレイヤーデータを保存
     */
    public void savePlayerData(PlayerData data) {
        try {
            plugin.getPlayerDataStorage().savePlayer(data);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getUuid(), e);
        }
    }

    /**
     * プレイヤーのログイン処理
     */
    public void handlePlayerJoin(Player player) {
        PlayerData data = getOrCreatePlayerData(player);
        data.setName(player.getName()); // 名前が変わっている可能性
        data.updateLastSeen();
        savePlayerData(data);

        // Murderer状態の場合は名前の色を更新
        updatePlayerNameColor(player);

        plugin.getLogger().fine("Player joined: " + player.getName() + " (" + player.getUniqueId() + ")");
    }

    /**
     * プレイヤーのログアウト処理
     */
    public void handlePlayerQuit(Player player) {
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            data.updateLastSeen();
            savePlayerData(data);
        }

        // ゲーム中でなければキャッシュから削除
        if (!plugin.getGameManager().isGameRunning()) {
            playerDataCache.remove(player.getUniqueId());
        }

        plugin.getLogger().fine("Player quit: " + player.getName());
    }

    /**
     * プレイヤーにポイントを追加
     */
    public void addPoints(UUID uuid, int points) {
        getPlayerData(uuid).ifPresent(data -> {
            data.addPoints(points);
            savePlayerData(data);
        });
    }

    /**
     * プレイヤーのキルをカウント
     */
    public void incrementKills(UUID uuid) {
        getPlayerData(uuid).ifPresent(data -> {
            data.incrementKills();
            savePlayerData(data);
        });
    }

    /**
     * プレイヤーのデスをカウント
     */
    public void incrementDeaths(UUID uuid) {
        getPlayerData(uuid).ifPresent(data -> {
            data.incrementDeaths();
            savePlayerData(data);
        });
    }

    /**
     * プレイヤーをMurdererに設定（累積、最大60分）
     * @return 初めてMurdererになった場合true、既にMurdererの場合false
     */
    public boolean setMurderer(UUID uuid, int durationSeconds) {
        final boolean[] isFirstTime = {false};

        getPlayerData(uuid).ifPresent(data -> {
            long now = System.currentTimeMillis() / 1000;
            long maxUntil = now + (60 * 60); // 最大60分後

            if (!data.isMurdererActive()) {
                // 初めてMurdererになる
                isFirstTime[0] = true;
                data.setMurderer(true);
                long until = now + durationSeconds;
                data.setMurdererUntil(Math.min(until, maxUntil));
                plugin.getLogger().info("Player " + uuid + " became murderer for the first time (" + durationSeconds + " seconds)");
            } else {
                // 既にMurderer - 時間を追加
                long currentUntil = data.getMurdererUntil();
                long newUntil = currentUntil + durationSeconds;

                // 60分を超えないようにキャップ
                if (newUntil > maxUntil) {
                    newUntil = maxUntil;
                    plugin.getLogger().info("Player " + uuid + " murderer time capped at 60 minutes");
                } else {
                    long addedMinutes = durationSeconds / 60;
                    long totalMinutes = (newUntil - now) / 60;
                    plugin.getLogger().info("Player " + uuid + " murderer time extended by " + addedMinutes + " minutes (total: " + totalMinutes + " minutes)");
                }

                data.setMurdererUntil(newUntil);
            }

            savePlayerData(data);
        });

        return isFirstTime[0];
    }

    /**
     * Murderer状態をクリア
     */
    public void clearMurderer(UUID uuid) {
        getPlayerData(uuid).ifPresent(data -> {
            data.clearMurderer();
            savePlayerData(data);

            // 名前の色を白に戻す
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                setPlayerNameWhite(player);
            }
        });
    }

    /**
     * プレイヤーの名前タグの色を更新（Murderer状態に応じて）
     */
    public void updatePlayerNameColor(Player player) {
        if (isMurderer(player.getUniqueId())) {
            setPlayerNameRed(player);
        } else {
            setPlayerNameWhite(player);
        }
    }

    /**
     * プレイヤーの名前を赤色に設定（ディスプレイネーム、タブリスト、頭上の名前）
     */
    private void setPlayerNameRed(Player player) {
        // ディスプレイネームとタブリストの名前
        player.setDisplayName("§c" + player.getName());
        player.setPlayerListName("§c" + player.getName());

        // 頭上の名前（NameTag）を赤くするためにメインScoreboardのTeamを使用
        org.bukkit.scoreboard.Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Murdererチームを取得または作成
        org.bukkit.scoreboard.Team murdererTeam = mainScoreboard.getTeam("murderer");
        if (murdererTeam == null) {
            murdererTeam = mainScoreboard.registerNewTeam("murderer");
            murdererTeam.setColor(org.bukkit.ChatColor.RED);
            murdererTeam.setPrefix("§c");
        }

        // プレイヤーをMurdererチームに追加
        if (!murdererTeam.hasEntry(player.getName())) {
            murdererTeam.addEntry(player.getName());
        }
    }

    /**
     * プレイヤーの名前を白色に戻す
     */
    private void setPlayerNameWhite(Player player) {
        // ディスプレイネームとタブリストの名前
        player.setDisplayName("§f" + player.getName());
        player.setPlayerListName("§f" + player.getName());

        // メインScoreboardのチームから削除
        org.bukkit.scoreboard.Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team murdererTeam = mainScoreboard.getTeam("murderer");
        if (murdererTeam != null && murdererTeam.hasEntry(player.getName())) {
            murdererTeam.removeEntry(player.getName());
        }
    }

    /**
     * プレイヤーがMurdererかチェック
     */
    public boolean isMurderer(UUID uuid) {
        return getPlayerData(uuid)
                .map(PlayerData::isMurdererActive)
                .orElse(false);
    }

    /**
     * 全プレイヤーのチーム割り当てをクリア
     */
    public void clearAllTeamAssignments() {
        try {
            plugin.getPlayerDataStorage().clearAllTeamAssignments();
            // キャッシュ内のデータも更新
            for (PlayerData data : playerDataCache.values()) {
                data.setTeamColor(null);
            }
            plugin.getLogger().info("Cleared all team assignments");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear team assignments", e);
        }
    }

    /**
     * 全プレイヤーのポイントをリセット
     */
    public void resetAllPoints() {
        try {
            plugin.getPlayerDataStorage().resetAllPoints();
            // キャッシュ内のデータも更新
            for (PlayerData data : playerDataCache.values()) {
                data.setPoints(0);
            }
            plugin.getLogger().info("Reset all player points");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset points", e);
        }
    }

    /**
     * トッププレイヤーを取得
     */
    public List<PlayerData> getTopPlayers(int limit) {
        try {
            return plugin.getPlayerDataStorage().getTopPlayers(limit);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top players", e);
            return Collections.emptyList();
        }
    }

    /**
     * チームのプレイヤーリストを取得
     */
    public List<PlayerData> getTeamPlayers(TeamColor teamColor) {
        try {
            return plugin.getPlayerDataStorage().loadPlayersByTeam(teamColor);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get team players", e);
            return Collections.emptyList();
        }
    }

    /**
     * 全プレイヤーデータを保存
     */
    public void saveAll() {
        int count = 0;
        for (PlayerData data : playerDataCache.values()) {
            try {
                plugin.getPlayerDataStorage().savePlayer(data);
                count++;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
            }
        }
        plugin.getLogger().info("Saved " + count + " player data entries");
    }

    /**
     * キャッシュをクリア
     */
    public void clearCache() {
        saveAll(); // 保存してからクリア
        playerDataCache.clear();
        plugin.getLogger().info("Player data cache cleared");
    }

    /**
     * クリーンアップ（プラグイン無効化時）
     */
    public void shutdown() {
        saveAll();
        playerDataCache.clear();
        plugin.getLogger().info("PlayerManager shutdown complete");
    }

    /**
     * キャッシュサイズを取得（デバッグ用）
     */
    public int getCacheSize() {
        return playerDataCache.size();
    }
}
