package com.candyrush.storage;

import com.candyrush.CandyRushPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Initializes and manages the SQLite database schema and connection pool
 */
public class DatabaseInitializer {

    private final CandyRushPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseInitializer(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the database connection pool and create tables if needed
     * @throws SQLException if database initialization fails
     */
    public void initialize() throws SQLException {
        // Create data directory if it doesn't exist
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Setup HikariCP connection pool
        HikariConfig config = new HikariConfig();
        String dbPath = new File(dataFolder, plugin.getConfigManager().getSqliteFile()).getAbsolutePath();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setDriverClassName("org.sqlite.JDBC");

        // Connection pool settings optimized for SQLite
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // SQLite-specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        // Create tables
        createTables();

        plugin.getLogger().info("Database initialized successfully at: " + dbPath);
    }

    /**
     * Create all required database tables
     * @throws SQLException if table creation fails
     */
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Players table - stores player data and team assignments
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS players (" +
                "    uuid TEXT PRIMARY KEY," +
                "    name TEXT NOT NULL," +
                "    team_color TEXT," +  // RED, BLUE, GREEN, YELLOW, or NULL
                "    points INTEGER DEFAULT 0," +
                "    kills INTEGER DEFAULT 0," +
                "    deaths INTEGER DEFAULT 0," +
                "    is_murderer INTEGER DEFAULT 0," +  // 0 = false, 1 = true
                "    murderer_until INTEGER," +  // Epoch timestamp when murderer status expires
                "    last_seen INTEGER," +  // Epoch timestamp of last login
                "    created_at INTEGER NOT NULL," +  // Epoch timestamp of first join
                "    updated_at INTEGER NOT NULL" +  // Epoch timestamp of last update
                ")"
            );

            // Game rounds table - tracks game sessions
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS game_rounds (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    started_at INTEGER NOT NULL," +  // Epoch timestamp
                "    ended_at INTEGER," +  // Epoch timestamp, NULL if ongoing
                "    winner_team TEXT," +  // RED, BLUE, GREEN, YELLOW, or NULL
                "    total_players INTEGER DEFAULT 0," +
                "    duration_seconds INTEGER," +  // Calculated when game ends
                "    created_at INTEGER NOT NULL" +
                ")"
            );

            // Team scores table - tracks team performance per round
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS team_scores (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    round_id INTEGER NOT NULL," +
                "    team_color TEXT NOT NULL," +  // RED, BLUE, GREEN, YELLOW
                "    final_points INTEGER DEFAULT 0," +
                "    total_kills INTEGER DEFAULT 0," +
                "    total_deaths INTEGER DEFAULT 0," +
                "    players_count INTEGER DEFAULT 0," +
                "    created_at INTEGER NOT NULL," +
                "    FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE" +
                ")"
            );

            // Player stats per round - detailed player performance
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    round_id INTEGER NOT NULL," +
                "    player_uuid TEXT NOT NULL," +
                "    team_color TEXT NOT NULL," +
                "    points_earned INTEGER DEFAULT 0," +
                "    kills INTEGER DEFAULT 0," +
                "    deaths INTEGER DEFAULT 0," +
                "    chests_opened INTEGER DEFAULT 0," +
                "    food_deposited INTEGER DEFAULT 0," +
                "    became_murderer INTEGER DEFAULT 0," +  // 0 = no, 1 = yes
                "    created_at INTEGER NOT NULL," +
                "    FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE," +
                "    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                ")"
            );

            // Treasure chests table - tracks spawned chests per game round
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS treasure_chests (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    round_id INTEGER NOT NULL," +
                "    world TEXT NOT NULL," +
                "    x INTEGER NOT NULL," +
                "    y INTEGER NOT NULL," +
                "    z INTEGER NOT NULL," +
                "    chest_type TEXT NOT NULL," +  // CHEST, BARREL, etc.
                "    spawned_at INTEGER NOT NULL," +  // Epoch timestamp
                "    FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE" +
                ")"
            );

            // Event NPCs table - tracks spawned NPCs per game round
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS event_npcs (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    round_id INTEGER NOT NULL," +
                "    entity_uuid TEXT," +  // UUID of the spawned entity
                "    world TEXT NOT NULL," +
                "    x REAL NOT NULL," +  // Use REAL for precise location
                "    y REAL NOT NULL," +
                "    z REAL NOT NULL," +
                "    yaw REAL NOT NULL," +
                "    pitch REAL NOT NULL," +
                "    npc_type TEXT NOT NULL," +  // EventNPC, FoodMerchant, etc.
                "    spawned_at INTEGER NOT NULL," +  // Epoch timestamp
                "    FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE" +
                ")"
            );

            // Create indexes for common queries
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_players_team ON players(team_color)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_players_points ON players(points DESC)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_game_rounds_started ON game_rounds(started_at DESC)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_team_scores_round ON team_scores(round_id)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_player_stats_round ON player_stats(round_id)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_player_stats_uuid ON player_stats(player_uuid)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_treasure_chests_round ON treasure_chests(round_id)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_event_npcs_round ON event_npcs(round_id)"
            );

            plugin.getLogger().info("Database tables created/verified successfully");
        }
    }

    /**
     * Get a connection from the pool
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Close the connection pool and release resources
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
    }

    /**
     * Check if the database is initialized and ready
     * @return true if initialized
     */
    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Execute a database health check
     * @return true if database is accessible
     */
    public boolean healthCheck() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database health check failed", e);
            return false;
        }
    }
}
