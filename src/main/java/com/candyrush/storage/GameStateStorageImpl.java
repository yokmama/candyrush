package com.candyrush.storage;

import com.candyrush.models.GameRound;
import com.candyrush.models.TeamColor;

import java.sql.*;
import java.util.*;

/**
 * SQLite implementation of GameStateStorage
 */
public class GameStateStorageImpl implements GameStateStorage {

    private final DatabaseInitializer databaseInitializer;

    public GameStateStorageImpl(DatabaseInitializer databaseInitializer) {
        this.databaseInitializer = databaseInitializer;
    }

    @Override
    public int createGameRound(GameRound gameRound) throws SQLException {
        String sql = "INSERT INTO game_rounds (started_at, ended_at, winner_team, total_players, duration_seconds, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, gameRound.getStartedAt());
            stmt.setObject(2, gameRound.getEndedAt());
            stmt.setString(3, gameRound.getWinnerTeam() != null ? gameRound.getWinnerTeam().name() : null);
            stmt.setInt(4, gameRound.getTotalPlayers());
            stmt.setObject(5, gameRound.getDurationSeconds());
            stmt.setLong(6, gameRound.getCreatedAt());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Creating game round failed, no ID obtained");
            }
        }
    }

    @Override
    public void updateGameRound(GameRound gameRound) throws SQLException {
        if (gameRound.getId() == null) {
            throw new IllegalArgumentException("Cannot update game round without ID");
        }

        String sql = "UPDATE game_rounds SET ended_at = ?, winner_team = ?, total_players = ?, duration_seconds = ? " +
                    "WHERE id = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, gameRound.getEndedAt());
            stmt.setString(2, gameRound.getWinnerTeam() != null ? gameRound.getWinnerTeam().name() : null);
            stmt.setInt(3, gameRound.getTotalPlayers());
            stmt.setObject(4, gameRound.getDurationSeconds());
            stmt.setInt(5, gameRound.getId());

            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<GameRound> loadGameRound(int roundId) throws SQLException {
        String sql = "SELECT * FROM game_rounds WHERE id = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roundId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(parseGameRound(rs));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<GameRound> getLatestGameRound() throws SQLException {
        String sql = "SELECT * FROM game_rounds ORDER BY started_at DESC LIMIT 1";

        try (Connection conn = databaseInitializer.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return Optional.of(parseGameRound(rs));
            }
        }

        return Optional.empty();
    }

    @Override
    public List<GameRound> getRecentGameRounds(int limit) throws SQLException {
        String sql = "SELECT * FROM game_rounds ORDER BY started_at DESC LIMIT ?";

        List<GameRound> rounds = new ArrayList<>();

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rounds.add(parseGameRound(rs));
                }
            }
        }

        return rounds;
    }

    @Override
    public void saveTeamScore(int roundId, TeamColor teamColor, int points, int kills, int deaths, int playersCount) throws SQLException {
        String sql = "INSERT INTO team_scores (round_id, team_color, final_points, total_kills, total_deaths, players_count, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roundId);
            stmt.setString(2, teamColor.name());
            stmt.setInt(3, points);
            stmt.setInt(4, kills);
            stmt.setInt(5, deaths);
            stmt.setInt(6, playersCount);
            stmt.setLong(7, System.currentTimeMillis() / 1000);

            stmt.executeUpdate();
        }
    }

    @Override
    public Map<TeamColor, TeamScore> loadTeamScores(int roundId) throws SQLException {
        String sql = "SELECT * FROM team_scores WHERE round_id = ?";

        Map<TeamColor, TeamScore> scores = new HashMap<>();

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roundId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TeamColor color = TeamColor.valueOf(rs.getString("team_color"));
                    int points = rs.getInt("final_points");
                    int kills = rs.getInt("total_kills");
                    int deaths = rs.getInt("total_deaths");
                    int playersCount = rs.getInt("players_count");

                    scores.put(color, new TeamScore(points, kills, deaths, playersCount));
                }
            }
        }

        return scores;
    }

    @Override
    public void savePlayerStats(int roundId, String playerUuid, TeamColor teamColor, int pointsEarned,
                                int kills, int deaths, int chestsOpened, int foodDeposited, boolean becameMurderer) throws SQLException {
        String sql = "INSERT INTO player_stats (round_id, player_uuid, team_color, points_earned, kills, deaths, " +
                    "chests_opened, food_deposited, became_murderer, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roundId);
            stmt.setString(2, playerUuid);
            stmt.setString(3, teamColor.name());
            stmt.setInt(4, pointsEarned);
            stmt.setInt(5, kills);
            stmt.setInt(6, deaths);
            stmt.setInt(7, chestsOpened);
            stmt.setInt(8, foodDeposited);
            stmt.setInt(9, becameMurderer ? 1 : 0);
            stmt.setLong(10, System.currentTimeMillis() / 1000);

            stmt.executeUpdate();
        }
    }

    @Override
    public int getTotalGamesPlayed() throws SQLException {
        String sql = "SELECT COUNT(*) FROM game_rounds WHERE ended_at IS NOT NULL";

        try (Connection conn = databaseInitializer.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    @Override
    public Map<TeamColor, Integer> getTeamWinCounts() throws SQLException {
        String sql = "SELECT winner_team, COUNT(*) as wins FROM game_rounds " +
                    "WHERE winner_team IS NOT NULL GROUP BY winner_team";

        Map<TeamColor, Integer> winCounts = new HashMap<>();

        // Initialize all teams with 0 wins
        for (TeamColor color : TeamColor.values()) {
            winCounts.put(color, 0);
        }

        try (Connection conn = databaseInitializer.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String teamStr = rs.getString("winner_team");
                int wins = rs.getInt("wins");
                TeamColor color = TeamColor.valueOf(teamStr);
                winCounts.put(color, wins);
            }
        }

        return winCounts;
    }

    /**
     * Parse GameRound from ResultSet
     * @param rs ResultSet positioned at a row
     * @return GameRound instance
     * @throws SQLException if parsing fails
     */
    private GameRound parseGameRound(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        long startedAt = rs.getLong("started_at");
        Long endedAt = rs.getObject("ended_at", Long.class);
        String winnerStr = rs.getString("winner_team");
        TeamColor winnerTeam = winnerStr != null ? TeamColor.valueOf(winnerStr) : null;
        int totalPlayers = rs.getInt("total_players");
        Integer durationSeconds = rs.getObject("duration_seconds", Integer.class);
        long createdAt = rs.getLong("created_at");

        return new GameRound(id, startedAt, endedAt, winnerTeam, totalPlayers, durationSeconds, createdAt);
    }
}
