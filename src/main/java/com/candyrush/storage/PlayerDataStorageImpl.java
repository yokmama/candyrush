package com.candyrush.storage;

import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite implementation of PlayerDataStorage
 */
public class PlayerDataStorageImpl implements PlayerDataStorage {

    private final DatabaseInitializer databaseInitializer;

    public PlayerDataStorageImpl(DatabaseInitializer databaseInitializer) {
        this.databaseInitializer = databaseInitializer;
    }

    @Override
    public Optional<PlayerData> loadPlayer(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(parsePlayerData(rs));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void savePlayer(PlayerData playerData) throws SQLException {
        // Note: DBカラム名は kills/deaths だが、実際の意味は pk/pkk
        // kills = PK (Player Kill), deaths = PKK (Player Killer Kill)
        String sql = "INSERT INTO players (uuid, name, team_color, points, kills, deaths, " +
                    "is_murderer, murderer_until, last_seen, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET " +
                    "name = excluded.name, " +
                    "team_color = excluded.team_color, " +
                    "points = excluded.points, " +
                    "kills = excluded.kills, " +
                    "deaths = excluded.deaths, " +
                    "is_murderer = excluded.is_murderer, " +
                    "murderer_until = excluded.murderer_until, " +
                    "last_seen = excluded.last_seen, " +
                    "updated_at = excluded.updated_at";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerData.getUuid().toString());
            stmt.setString(2, playerData.getName());
            stmt.setString(3, playerData.getTeamColor() != null ? playerData.getTeamColor().name() : null);
            stmt.setInt(4, playerData.getPoints());
            stmt.setInt(5, playerData.getPk());     // kills -> PK
            stmt.setInt(6, playerData.getPkk());    // deaths -> PKK
            stmt.setInt(7, playerData.isMurderer() ? 1 : 0);
            stmt.setLong(8, playerData.getMurdererUntil());
            stmt.setLong(9, playerData.getLastSeen());
            stmt.setLong(10, playerData.getCreatedAt());
            stmt.setLong(11, playerData.getUpdatedAt());

            stmt.executeUpdate();
        }
    }

    @Override
    public void deletePlayer(UUID uuid) throws SQLException {
        String sql = "DELETE FROM players WHERE uuid = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<PlayerData> loadPlayersByTeam(TeamColor teamColor) throws SQLException {
        String sql = "SELECT * FROM players WHERE team_color = ? ORDER BY points DESC";

        List<PlayerData> players = new ArrayList<>();

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamColor.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    players.add(parsePlayerData(rs));
                }
            }
        }

        return players;
    }

    @Override
    public List<PlayerData> getTopPlayers(int limit) throws SQLException {
        String sql = "SELECT * FROM players ORDER BY points DESC LIMIT ?";

        List<PlayerData> players = new ArrayList<>();

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    players.add(parsePlayerData(rs));
                }
            }
        }

        return players;
    }

    @Override
    public List<PlayerData> getActiveMurderers() throws SQLException {
        long now = System.currentTimeMillis() / 1000;
        String sql = "SELECT * FROM players WHERE is_murderer = 1 AND murderer_until > ?";

        List<PlayerData> murderers = new ArrayList<>();

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, now);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    murderers.add(parsePlayerData(rs));
                }
            }
        }

        return murderers;
    }

    @Override
    public void clearAllTeamAssignments() throws SQLException {
        String sql = "UPDATE players SET team_color = NULL, updated_at = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();
        }
    }

    @Override
    public void resetAllPoints() throws SQLException {
        String sql = "UPDATE players SET points = 0, updated_at = ?";

        try (Connection conn = databaseInitializer.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, System.currentTimeMillis() / 1000);
            stmt.executeUpdate();
        }
    }

    @Override
    public int getTotalPlayerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM players";

        try (Connection conn = databaseInitializer.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Parse PlayerData from ResultSet
     * @param rs ResultSet positioned at a row
     * @return PlayerData instance
     * @throws SQLException if parsing fails
     */
    private PlayerData parsePlayerData(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        String teamColorStr = rs.getString("team_color");
        TeamColor teamColor = teamColorStr != null ? TeamColor.valueOf(teamColorStr) : null;
        int points = rs.getInt("points");
        // Note: DBカラム名は kills/deaths だが、実際の意味は pk/pkk
        int pk = rs.getInt("kills");      // kills -> PK
        int pkk = rs.getInt("deaths");    // deaths -> PKK
        boolean isMurderer = rs.getInt("is_murderer") == 1;
        long murdererUntil = rs.getLong("murderer_until");
        long lastSeen = rs.getLong("last_seen");
        long createdAt = rs.getLong("created_at");
        long updatedAt = rs.getLong("updated_at");

        return new PlayerData(uuid, name, teamColor, points, pk, pkk,
                            isMurderer, murdererUntil, lastSeen, createdAt, updatedAt);
    }
}
