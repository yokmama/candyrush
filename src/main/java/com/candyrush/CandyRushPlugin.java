package com.candyrush;

import com.candyrush.commands.ConvertCommand;
import com.candyrush.commands.DebugCommand;
import com.candyrush.commands.StatsCommand;
import com.candyrush.integration.MythicMobsIntegration;
import com.candyrush.listeners.*;
import com.candyrush.managers.*;
import com.candyrush.storage.DatabaseInitializer;
import com.candyrush.storage.GameStateStorage;
import com.candyrush.storage.GameStateStorageImpl;
import com.candyrush.storage.PlayerDataStorage;
import com.candyrush.storage.PlayerDataStorageImpl;
import com.candyrush.utils.ConfigManager;
import com.candyrush.utils.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * Main plugin class for CandyRush - Team-based point collection game
 */
public class CandyRushPlugin extends JavaPlugin {

    private static CandyRushPlugin instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseInitializer databaseInitializer;
    private PlayerDataStorage playerDataStorage;
    private GameStateStorage gameStateStorage;
    private MythicMobsIntegration mythicMobsIntegration;

    // Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private PlayerManager playerManager;
    private TreasureChestManager treasureChestManager;
    private PointConversionManager pointConversionManager;
    private EventNpcManager eventNpcManager;
    private BossManager bossManager;
    private ScoreboardManager scoreboardManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("=================================");
        getLogger().info("  CandyRush Plugin Enabling...  ");
        getLogger().info("=================================");

        // Load configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        getLogger().info("Configuration loaded");

        // Initialize language manager
        languageManager = new LanguageManager(this);
        getLogger().info("Language manager initialized (" + languageManager.getCurrentLanguage() + ")");

        // Check MythicMobs dependency (optional)
        boolean hasMythicMobs = getServer().getPluginManager().getPlugin("MythicMobs") != null;
        if (hasMythicMobs) {
            getLogger().info("MythicMobs dependency found");
        } else {
            getLogger().warning("MythicMobs not found! Boss and Event features will be disabled.");
        }

        // Initialize database
        databaseInitializer = new DatabaseInitializer(this);
        try {
            databaseInitializer.initialize();
            getLogger().info("Database initialized successfully");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize storage layers
        playerDataStorage = new PlayerDataStorageImpl(databaseInitializer);
        gameStateStorage = new GameStateStorageImpl(databaseInitializer);
        getLogger().info("Storage layers initialized");

        // Initialize MythicMobs integration (if available)
        if (hasMythicMobs) {
            mythicMobsIntegration = new MythicMobsIntegration(this);
            if (!mythicMobsIntegration.initialize()) {
                getLogger().warning("Failed to initialize MythicMobs integration");
                getLogger().warning("Boss and Event features will be disabled");
            } else {
                // Validate required mobs exist
                if (!mythicMobsIntegration.validateRequiredMobs()) {
                    getLogger().warning("Some required MythicMobs are missing - check warnings above");
                    getLogger().warning("The plugin will continue, but events/bosses may not work correctly");
                }
            }
        }

        // Initialize managers
        gameManager = new GameManager(this);
        teamManager = new TeamManager(this);
        playerManager = new PlayerManager(this);
        treasureChestManager = new TreasureChestManager(this);
        pointConversionManager = new PointConversionManager(this);
        eventNpcManager = new EventNpcManager(this);
        bossManager = new BossManager(this);
        scoreboardManager = new ScoreboardManager(this);
        shopManager = new ShopManager(this);

        gameManager.initialize();
        teamManager.initialize();
        playerManager.initialize();
        treasureChestManager.initialize();
        pointConversionManager.initialize();
        eventNpcManager.initialize();
        bossManager.initialize();
        scoreboardManager.initialize();
        getLogger().info("Game managers initialized");

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new TreasureChestListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new GoldItemConvertListener(this), this);
        getServer().getPluginManager().registerEvents(new EventNpcListener(this), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PvpListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(this), this);
        getServer().getPluginManager().registerEvents(shopManager, this);
        getLogger().info("Event listeners registered");

        // Register commands
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("stats").setTabCompleter(new com.candyrush.commands.StatsCommandTabCompleter());

        getCommand("convert").setExecutor(new ConvertCommand(this));

        getCommand("candyrush").setExecutor(new DebugCommand(this));
        getCommand("candyrush").setTabCompleter(new com.candyrush.commands.DebugCommandTabCompleter());

        getCommand("shop").setExecutor(new com.candyrush.commands.ShopCommand(this));
        getLogger().info("Commands registered");

        getLogger().info("=================================");
        getLogger().info(" CandyRush Plugin Enabled!     ");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info(" CandyRush Plugin Disabling... ");
        getLogger().info("=================================");

        // Shutdown managers
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (teamManager != null) {
            teamManager.shutdown();
        }
        if (playerManager != null) {
            playerManager.shutdown();
        }
        if (treasureChestManager != null) {
            treasureChestManager.shutdown();
        }
        if (pointConversionManager != null) {
            pointConversionManager.shutdown();
        }
        if (eventNpcManager != null) {
            eventNpcManager.shutdown();
        }
        if (bossManager != null) {
            bossManager.shutdown();
        }

        // Close database connections
        if (databaseInitializer != null) {
            databaseInitializer.close();
        }

        getLogger().info("=================================");
        getLogger().info("  CandyRush Plugin Disabled!   ");
        getLogger().info("=================================");
    }

    /**
     * Get the plugin instance
     * @return Plugin instance
     */
    public static CandyRushPlugin getInstance() {
        return instance;
    }

    /**
     * Get the configuration manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the language manager
     * @return LanguageManager instance
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Get the database initializer
     * @return DatabaseInitializer instance
     */
    public DatabaseInitializer getDatabaseInitializer() {
        return databaseInitializer;
    }

    /**
     * Get the player data storage
     * @return PlayerDataStorage instance
     */
    public PlayerDataStorage getPlayerDataStorage() {
        return playerDataStorage;
    }

    /**
     * Get the game state storage
     * @return GameStateStorage instance
     */
    public GameStateStorage getGameStateStorage() {
        return gameStateStorage;
    }

    /**
     * Get the MythicMobs integration
     * @return MythicMobsIntegration instance
     */
    public MythicMobsIntegration getMythicMobsIntegration() {
        return mythicMobsIntegration;
    }

    /**
     * Get the game manager
     * @return GameManager instance
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Get the team manager
     * @return TeamManager instance
     */
    public TeamManager getTeamManager() {
        return teamManager;
    }

    /**
     * Get the player manager
     * @return PlayerManager instance
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * Get the treasure chest manager
     * @return TreasureChestManager instance
     */
    public TreasureChestManager getTreasureChestManager() {
        return treasureChestManager;
    }

    /**
     * Get the point conversion manager
     * @return PointConversionManager instance
     */
    public PointConversionManager getPointConversionManager() {
        return pointConversionManager;
    }

    /**
     * Get the event NPC manager
     * @return EventNpcManager instance
     */
    public EventNpcManager getEventNpcManager() {
        return eventNpcManager;
    }

    /**
     * Get the boss manager
     * @return BossManager instance
     */
    public BossManager getBossManager() {
        return bossManager;
    }

    /**
     * Get the scoreboard manager
     * @return ScoreboardManager instance
     */
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Get the shop manager
     * @return ShopManager instance
     */
    public ShopManager getShopManager() {
        return shopManager;
    }
}
