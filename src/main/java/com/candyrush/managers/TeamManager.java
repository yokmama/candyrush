package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.Team;
import com.candyrush.models.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 4つのチーム（赤・青・緑・黄）を管理するマネージャー
 * チーム分け、ポイント管理、スポーン地点管理を担当
 */
public class TeamManager {

    private final CandyRushPlugin plugin;
    private final Map<TeamColor, Team> teams;

    public TeamManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.teams = new EnumMap<>(TeamColor.class);
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        // 4つのチームを作成
        for (TeamColor color : TeamColor.values()) {
            teams.put(color, new Team(color));
        }

        plugin.getLogger().info("TeamManager initialized - 4 teams created");
    }

    /**
     * 特定のチームを取得
     */
    public Team getTeam(TeamColor color) {
        return teams.get(color);
    }

    /**
     * 全チームを取得
     */
    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    /**
     * プレイヤーのチームを取得
     */
    public Optional<Team> getPlayerTeam(UUID playerUuid) {
        for (Team team : teams.values()) {
            if (team.hasPlayer(playerUuid)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    /**
     * プレイヤーをチームに割り当て
     */
    public void assignPlayerToTeam(UUID playerUuid, TeamColor teamColor) {
        // 既存のチームから削除
        removePlayerFromAllTeams(playerUuid);

        // 新しいチームに追加
        Team team = teams.get(teamColor);
        if (team != null) {
            team.addPlayer(playerUuid);
            plugin.getLogger().fine("Player " + playerUuid + " assigned to " + teamColor + " team");
        }

        // PlayerDataのteamColorも更新
        plugin.getPlayerManager().getPlayerData(playerUuid).ifPresent(data -> {
            data.setTeamColor(teamColor);
            plugin.getPlayerManager().savePlayerData(data);
        });

        // オンラインプレイヤーの場合、Scoreboardチーム色も更新
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            plugin.getPlayerManager().updatePlayerTeamColor(player);
        }
    }

    /**
     * プレイヤーを全チームから削除
     */
    public void removePlayerFromAllTeams(UUID playerUuid) {
        for (Team team : teams.values()) {
            team.removePlayer(playerUuid);
        }

        // PlayerDataのteamColorもクリア
        plugin.getPlayerManager().getPlayerData(playerUuid).ifPresent(data -> {
            data.setTeamColor(null);
            plugin.getPlayerManager().savePlayerData(data);
        });
    }

    /**
     * 全プレイヤーを均等に3チーム（BLUE, GREEN, YELLOW）に振り分け
     * RED チームは Murderer 専用のため除外
     */
    public void distributePlayersEvenly(Collection<Player> players) {
        // 全チームをリセット
        resetAllTeams();

        List<Player> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList); // ランダムに並び替え

        // ゲームチームのみ (RED を除外)
        TeamColor[] gameTeams = {TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW};
        int teamIndex = 0;

        // 順番にチームに割り当て
        for (Player player : playerList) {
            TeamColor color = gameTeams[teamIndex % gameTeams.length];
            assignPlayerToTeam(player.getUniqueId(), color);
            teamIndex++;
        }

        plugin.getLogger().info("Distributed " + players.size() + " players across 3 teams (BLUE, GREEN, YELLOW)");

        // 各ゲームチームの人数をログ
        for (TeamColor color : gameTeams) {
            Team team = teams.get(color);
            plugin.getLogger().info(color + " team: " + team.getPlayerCount() + " players");
        }
    }

    /**
     * 最も人数が少ないゲームチームを取得（バランス調整用）
     * RED チームは Murderer 専用のため除外
     */
    public TeamColor getSmallestTeam() {
        TeamColor[] gameTeams = {TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW};

        TeamColor smallest = TeamColor.BLUE;
        int minCount = Integer.MAX_VALUE;

        for (TeamColor color : gameTeams) {
            Team team = teams.get(color);
            if (team != null && team.getPlayerCount() < minCount) {
                minCount = team.getPlayerCount();
                smallest = color;
            }
        }

        return smallest;
    }

    /**
     * チームにポイントを追加
     */
    public void addTeamPoints(TeamColor color, int points) {
        Team team = teams.get(color);
        if (team != null) {
            team.addPoints(points);
        }
    }

    /**
     * チームのキルをカウント
     */
    public void incrementTeamKills(TeamColor color) {
        Team team = teams.get(color);
        if (team != null) {
            team.incrementKills();
        }
    }

    /**
     * チームのデスをカウント
     */
    public void incrementTeamDeaths(TeamColor color) {
        Team team = teams.get(color);
        if (team != null) {
            team.incrementDeaths();
        }
    }

    /**
     * 最高ポイントのチームを取得（勝者判定）
     */
    public Optional<TeamColor> getWinningTeam() {
        return teams.values().stream()
                .max(Comparator.comparingInt(Team::getPoints))
                .map(Team::getColor);
    }

    /**
     * 同点チェック（引き分け判定）
     */
    public boolean isTie() {
        if (teams.isEmpty()) {
            return true;
        }

        int maxPoints = teams.values().stream()
                .mapToInt(Team::getPoints)
                .max()
                .orElse(0);

        long teamsWithMaxPoints = teams.values().stream()
                .filter(team -> team.getPoints() == maxPoints)
                .count();

        return teamsWithMaxPoints > 1;
    }

    /**
     * チームランキングを取得（ポイント順）
     */
    public List<Team> getTeamRanking() {
        List<Team> ranking = new ArrayList<>(teams.values());
        ranking.sort((t1, t2) -> Integer.compare(t2.getPoints(), t1.getPoints()));
        return ranking;
    }

    /**
     * 全チームをリセット（ゲーム終了時）
     */
    public void resetAllTeams() {
        for (Team team : teams.values()) {
            team.reset();
        }
        plugin.getLogger().info("All teams reset");
    }

    /**
     * チーム統計情報を取得（デバッグ用）
     */
    public Map<TeamColor, Map<String, Object>> getTeamStatistics() {
        Map<TeamColor, Map<String, Object>> stats = new EnumMap<>(TeamColor.class);

        for (TeamColor color : TeamColor.values()) {
            Team team = teams.get(color);
            Map<String, Object> teamStats = new HashMap<>();
            teamStats.put("players", team.getPlayerCount());
            teamStats.put("points", team.getPoints());
            teamStats.put("kills", team.getKills());
            teamStats.put("deaths", team.getDeaths());
            teamStats.put("kd_ratio", team.getKDRatio());
            stats.put(color, teamStats);
        }

        return stats;
    }

    /**
     * チーム情報をデータベースに保存
     * @param roundId ゲームラウンドID
     */
    public void saveTeamScoresToDatabase(int roundId) {
        for (Team team : teams.values()) {
            try {
                plugin.getGameStateStorage().saveTeamScore(
                    roundId,
                    team.getColor(),
                    team.getPoints(),
                    team.getKills(),
                    team.getDeaths(),
                    team.getPlayerCount()
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save team score for " + team.getColor() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Team scores saved to database for round " + roundId);
    }

    /**
     * 全チームの合計ポイントを取得
     */
    public int getTotalPoints() {
        return teams.values().stream()
                .mapToInt(Team::getPoints)
                .sum();
    }

    /**
     * 特定チームのポイントを取得
     */
    public int getTeamPoints(TeamColor teamColor) {
        Team team = teams.get(teamColor);
        return team != null ? team.getPoints() : 0;
    }

    /**
     * チーム拠点の座標を取得
     * マップの4隅にチーム拠点を配置
     * @param world ワールド
     * @param center マップ中心座標
     * @param mapRadius マップ半径
     * @param teamColor チームカラー
     * @return チーム拠点の座標
     */
    public Location getTeamSpawnLocation(World world, Location center, int mapRadius, TeamColor teamColor) {
        int offsetX = 0;
        int offsetZ = 0;
        int offset = mapRadius - 20; // 境界から20ブロック内側

        switch (teamColor) {
            case RED:
                // 北東（+X, -Z）
                offsetX = offset;
                offsetZ = -offset;
                break;
            case BLUE:
                // 北西（-X, -Z）
                offsetX = -offset;
                offsetZ = -offset;
                break;
            case GREEN:
                // 南西（-X, +Z）
                offsetX = -offset;
                offsetZ = offset;
                break;
            case YELLOW:
                // 南東（+X, +Z）
                offsetX = offset;
                offsetZ = offset;
                break;
        }

        // Y座標は地面を探す
        int x = center.getBlockX() + offsetX;
        int z = center.getBlockZ() + offsetZ;
        int y = world.getHighestBlockYAt(x, z) + 1;

        return new Location(world, x + 0.5, y, z + 0.5, 0, 0); // 中央にスポーン
    }

    /**
     * クリーンアップ（プラグイン無効化時）
     */
    public void shutdown() {
        resetAllTeams();
        plugin.getLogger().info("TeamManager shutdown complete");
    }
}
