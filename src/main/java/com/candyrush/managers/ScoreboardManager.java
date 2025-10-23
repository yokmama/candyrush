package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.GameState;
import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.UUID;

/**
 * サイドバースコアボードの管理
 * プレイヤー情報、チーム情報、ゲーム情報を表示
 */
public class ScoreboardManager {

    private final CandyRushPlugin plugin;
    private BukkitTask updateTask;

    public ScoreboardManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        // スコアボード更新タスクを開始（1秒ごと）
        startUpdateTask();
        plugin.getLogger().info("ScoreboardManager initialized");
    }

    /**
     * プレイヤーにスコアボードを設定
     */
    public void setupScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
            "candyrush",
            "dummy",
            MessageUtils.colorize("&e&l⚡ &6&lCandy Rush &e&l⚡")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        updatePlayerScoreboard(player);
    }

    /**
     * プレイヤーのスコアボードを更新
     */
    public void updatePlayerScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        Objective objective = scoreboard.getObjective("candyrush");
        if (objective == null) {
            return;
        }

        // 既存のスコアをクリア
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        GameState gameState = plugin.getGameManager().getCurrentState();

        // PlayerDataを取得
        java.util.Optional<PlayerData> playerDataOpt = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (!playerDataOpt.isPresent()) {
            return;
        }

        PlayerData playerData = playerDataOpt.get();

        int line = 15;

        // 空行
        setScore(objective, " ", line--);

        // ゲーム状態別の表示
        switch (gameState) {
            case WAITING:
                setScore(objective, MessageUtils.colorize("&7状態: &e待機中"), line--);
                setScore(objective, MessageUtils.colorize("&7プレイヤー: &a" + Bukkit.getOnlinePlayers().size() + "&7/&a" + plugin.getConfigManager().getMinPlayers()), line--);
                break;

            case COUNTDOWN:
                setScore(objective, MessageUtils.colorize("&7状態: &eカウントダウン"), line--);
                break;

            case RUNNING:
                // ゲーム時間
                int gameTimeRemaining = getGameTimeRemaining();
                setScore(objective, MessageUtils.colorize("&7残り時間: &c" + formatTime(gameTimeRemaining)), line--);
                setScore(objective, MessageUtils.colorize("  "), line--);

                // チーム情報
                TeamColor teamColor = playerData.getTeamColor();
                if (teamColor != null) {
                    setScore(objective, MessageUtils.colorize("&7チーム: " + teamColor.getFormattedName()), line--);
                } else {
                    setScore(objective, MessageUtils.colorize("&7チーム: &8なし"), line--);
                }

                setScore(objective, MessageUtils.colorize("   "), line--);

                // プレイヤー統計
                setScore(objective, MessageUtils.colorize("&e━━━ &6統計 &e━━━"), line--);
                setScore(objective, MessageUtils.colorize("&7ポイント: &a" + playerData.getPoints()), line--);
                setScore(objective, MessageUtils.colorize("&7キル: &c" + playerData.getKills()), line--);
                setScore(objective, MessageUtils.colorize("&7デス: &7" + playerData.getDeaths()), line--);

                // 殺人者状態
                if (playerData.isMurderer()) {
                    setScore(objective, MessageUtils.colorize("    "), line--);
                    setScore(objective, MessageUtils.colorize("&4&l⚠ 殺人者 ⚠"), line--);
                }

                break;

            case COOLDOWN:
                setScore(objective, MessageUtils.colorize("&7状態: &eクールダウン"), line--);
                setScore(objective, MessageUtils.colorize("  "), line--);

                // 最終スコア
                setScore(objective, MessageUtils.colorize("&e━━━ &6最終スコア &e━━━"), line--);
                setScore(objective, MessageUtils.colorize("&7ポイント: &a" + playerData.getPoints()), line--);
                setScore(objective, MessageUtils.colorize("&7キル: &c" + playerData.getKills()), line--);
                setScore(objective, MessageUtils.colorize("&7デス: &7" + playerData.getDeaths()), line--);
                break;
        }

        setScore(objective, MessageUtils.colorize("     "), line--);

        // チーム順位（ゲーム中のみ）
        if (gameState == GameState.RUNNING) {
            setScore(objective, MessageUtils.colorize("&e━━━ &6チーム順位 &e━━━"), line--);
            displayTeamRankings(objective, line);
        }
    }

    /**
     * チーム順位を表示
     */
    private void displayTeamRankings(Objective objective, int startLine) {
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager == null) {
            return;
        }

        int line = startLine;

        // 各チームのポイントを取得してソート
        java.util.List<TeamColor> teams = new java.util.ArrayList<>(java.util.Arrays.asList(TeamColor.values()));
        teams.sort((t1, t2) -> Integer.compare(
            teamManager.getTeamPoints(t2),
            teamManager.getTeamPoints(t1)
        ));

        // 上位3チームを表示
        int rank = 1;
        for (TeamColor team : teams) {
            if (rank > 3) break;

            int points = teamManager.getTeamPoints(team);
            String medal = rank == 1 ? "&6🥇" : rank == 2 ? "&7🥈" : "&c🥉";
            setScore(objective, MessageUtils.colorize(
                medal + " " + team.getFormattedName() + " &7: &a" + points
            ), line--);
            rank++;
        }
    }

    /**
     * スコアを設定
     */
    private void setScore(Objective objective, String text, int score) {
        // スコアボードは同じテキストを複数行に設定できないため、
        // 空白を使って区別する
        String entry = text;
        if (entry.length() > 40) {
            entry = entry.substring(0, 40);
        }

        Score scoreEntry = objective.getScore(entry);
        scoreEntry.setScore(score);
    }

    /**
     * 全プレイヤーのスコアボードを更新
     */
    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
        }
    }

    /**
     * スコアボード更新タスクを開始
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateAllScoreboards();
        }, 20L, 20L); // 1秒ごとに更新
    }

    /**
     * プレイヤーのスコアボードをクリア
     */
    public void clearScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    /**
     * 残りゲーム時間を取得
     */
    private int getGameTimeRemaining() {
        return plugin.getGameManager().getGameTimeRemaining();
    }

    /**
     * 時間をフォーマット (MM:SS)
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * クリーンアップ
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // 全プレイヤーのスコアボードをクリア
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearScoreboard(player);
        }

        plugin.getLogger().info("ScoreboardManager shutdown complete");
    }
}
