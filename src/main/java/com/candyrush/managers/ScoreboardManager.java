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
    private Scoreboard mainScoreboard; // サーバー全体で共有するメインScoreboard

    public ScoreboardManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        // メインScoreboardを作成
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            mainScoreboard = manager.getMainScoreboard();

            // murdererチームとnormalチームを作成
            setupMainScoreboardTeams();
        }

        // スコアボード更新タスクを開始（1秒ごと）
        startUpdateTask();
        plugin.getLogger().info("ScoreboardManager initialized");
    }

    /**
     * メインScoreboardに4つのチームカラーを設定
     * ゲームチーム: BLUE, GREEN, YELLOW (3チーム)
     * Murdererチーム: RED (殺人者専用)
     */
    private void setupMainScoreboardTeams() {
        // BLUEチーム（青色）
        Team blueTeam = mainScoreboard.getTeam("blue");
        if (blueTeam == null) {
            blueTeam = mainScoreboard.registerNewTeam("blue");
        }
        blueTeam.setColor(org.bukkit.ChatColor.BLUE);
        blueTeam.setPrefix("§9");
        blueTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        // GREENチーム（緑色）
        Team greenTeam = mainScoreboard.getTeam("green");
        if (greenTeam == null) {
            greenTeam = mainScoreboard.registerNewTeam("green");
        }
        greenTeam.setColor(org.bukkit.ChatColor.GREEN);
        greenTeam.setPrefix("§a");
        greenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        // YELLOWチーム（黄色）
        Team yellowTeam = mainScoreboard.getTeam("yellow");
        if (yellowTeam == null) {
            yellowTeam = mainScoreboard.registerNewTeam("yellow");
        }
        yellowTeam.setColor(org.bukkit.ChatColor.YELLOW);
        yellowTeam.setPrefix("§e");
        yellowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        // REDチーム（赤色 - Murderer専用）
        Team redTeam = mainScoreboard.getTeam("red");
        if (redTeam == null) {
            redTeam = mainScoreboard.registerNewTeam("red");
        }
        redTeam.setColor(org.bukkit.ChatColor.RED);
        redTeam.setPrefix("§c");
        redTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        plugin.getLogger().info("Main scoreboard teams created: blue, green, yellow (game teams) and red (murderer team)");
    }

    /**
     * プレイヤーにスコアボードを設定
     *
     * 重要: 個別Scoreboardを作成してサイドバー表示用に使う
     * ただし、チーム（名前の色）はメインScoreboardで管理
     */
    public void setupScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        // 個別Scoreboardを作成（サイドバー表示用）
        Scoreboard playerScoreboard = manager.getNewScoreboard();

        // サイドバー用のObjectiveを作成
        Objective objective = playerScoreboard.registerNewObjective(
            "candyrush",
            "dummy",
            MessageUtils.colorize("&e&l⚡ &6&lCandy Rush &e&l⚡")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // メインScoreboardのチームをコピー（名前の色が見えるように）
        copyTeamsFromMainScoreboard(playerScoreboard);

        // 個別Scoreboardを設定
        player.setScoreboard(playerScoreboard);

        updatePlayerScoreboard(player);
    }

    /**
     * メインScoreboardのチーム情報を個別Scoreboardにコピー
     * 4つのチームカラー (BLUE, GREEN, YELLOW, RED) をすべてコピー
     */
    private void copyTeamsFromMainScoreboard(Scoreboard playerScoreboard) {
        if (mainScoreboard == null) {
            return;
        }

        // BLUEチームをコピー
        Team mainBlueTeam = mainScoreboard.getTeam("blue");
        if (mainBlueTeam != null) {
            Team blueTeam = playerScoreboard.getTeam("blue");
            if (blueTeam == null) {
                blueTeam = playerScoreboard.registerNewTeam("blue");
            }
            blueTeam.setColor(org.bukkit.ChatColor.BLUE);
            blueTeam.setPrefix("§9");
            blueTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            // メンバーをコピー
            for (String entry : mainBlueTeam.getEntries()) {
                if (!blueTeam.hasEntry(entry)) {
                    blueTeam.addEntry(entry);
                }
            }
        }

        // GREENチームをコピー
        Team mainGreenTeam = mainScoreboard.getTeam("green");
        if (mainGreenTeam != null) {
            Team greenTeam = playerScoreboard.getTeam("green");
            if (greenTeam == null) {
                greenTeam = playerScoreboard.registerNewTeam("green");
            }
            greenTeam.setColor(org.bukkit.ChatColor.GREEN);
            greenTeam.setPrefix("§a");
            greenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            // メンバーをコピー
            for (String entry : mainGreenTeam.getEntries()) {
                if (!greenTeam.hasEntry(entry)) {
                    greenTeam.addEntry(entry);
                }
            }
        }

        // YELLOWチームをコピー
        Team mainYellowTeam = mainScoreboard.getTeam("yellow");
        if (mainYellowTeam != null) {
            Team yellowTeam = playerScoreboard.getTeam("yellow");
            if (yellowTeam == null) {
                yellowTeam = playerScoreboard.registerNewTeam("yellow");
            }
            yellowTeam.setColor(org.bukkit.ChatColor.YELLOW);
            yellowTeam.setPrefix("§e");
            yellowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            // メンバーをコピー
            for (String entry : mainYellowTeam.getEntries()) {
                if (!yellowTeam.hasEntry(entry)) {
                    yellowTeam.addEntry(entry);
                }
            }
        }

        // REDチームをコピー（Murderer専用）
        Team mainRedTeam = mainScoreboard.getTeam("red");
        if (mainRedTeam != null) {
            Team redTeam = playerScoreboard.getTeam("red");
            if (redTeam == null) {
                redTeam = playerScoreboard.registerNewTeam("red");
            }
            redTeam.setColor(org.bukkit.ChatColor.RED);
            redTeam.setPrefix("§c");
            redTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            // メンバーをコピー
            for (String entry : mainRedTeam.getEntries()) {
                if (!redTeam.hasEntry(entry)) {
                    redTeam.addEntry(entry);
                }
            }
        }
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
                setScore(objective, MessageUtils.colorize("&7PK: &c" + playerData.getPk()), line--);
                setScore(objective, MessageUtils.colorize("&7PKK: &6" + playerData.getPkk()), line--);

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
                setScore(objective, MessageUtils.colorize("&7PK: &c" + playerData.getPk()), line--);
                setScore(objective, MessageUtils.colorize("&7PKK: &6" + playerData.getPkk()), line--);
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
     * 特定チームのスコアボードを更新
     */
    public void updateTeamScoreboards(TeamColor teamColor) {
        com.candyrush.models.Team team = plugin.getTeamManager().getTeam(teamColor);
        if (team == null) {
            plugin.getLogger().warning("Cannot update scoreboards for team " + teamColor + " - team not found");
            return;
        }

        int updatedCount = 0;
        for (UUID uuid : team.getPlayerUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayerScoreboard(player);
                updatedCount++;
            }
        }

        plugin.getLogger().fine("Updated scoreboards for " + updatedCount + " players in " + teamColor + " team");
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
