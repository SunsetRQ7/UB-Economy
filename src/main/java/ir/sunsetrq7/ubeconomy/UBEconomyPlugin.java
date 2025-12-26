package ir.sunsetrq7.ubeconomy;

import ir.sunsetrq7.ubeconomy.command.CommandManager;
import ir.sunsetrq7.ubeconomy.database.DatabaseManager;
import ir.sunsetrq7.ubeconomy.event.PlayerEventListener;
import ir.sunsetrq7.ubeconomy.event.TransactionEventListener;
import ir.sunsetrq7.ubeconomy.config.ConfigManager;
import ir.sunsetrq7.ubeconomy.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * UBEconomyPlugin - Main plugin entry point for UB-Economy
 * 
 * This plugin provides a comprehensive economy system for Bukkit/Spigot servers.
 * It handles player balance management, transactions, and economic interactions.
 * 
 * @author SunsetRQ7
 * @version 1.0.0
 */
public class UBEconomyPlugin extends JavaPlugin {

    // Plugin components
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private CommandManager commandManager;

    // Plugin instance
    private static UBEconomyPlugin instance;

    @Override
    public void onEnable() {
        // Set plugin instance
        instance = this;

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("       UB-Economy Plugin Loading");
        getLogger().info("═══════════════════════════════════════");

        try {
            // 1. Initialize Configuration
            initializeConfiguration();

            // 2. Initialize Database
            initializeDatabase();

            // 3. Initialize Economy Manager
            initializeEconomyManager();

            // 4. Register Commands
            registerCommands();

            // 5. Register Event Listeners
            registerEventListeners();

            // 6. Load economy data
            loadEconomyData();

            getLogger().info("═══════════════════════════════════════");
            getLogger().info("    UB-Economy Plugin Enabled Successfully");
            getLogger().info("═══════════════════════════════════════");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable UB-Economy plugin!", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("       UB-Economy Plugin Disabling");
        getLogger().info("═══════════════════════════════════════");

        try {
            // Save economy data
            if (economyManager != null) {
                economyManager.saveAllData();
                getLogger().info("Economy data saved successfully");
            }

            // Close database connection
            if (databaseManager != null) {
                databaseManager.close();
                getLogger().info("Database connection closed");
            }

            getLogger().info("═══════════════════════════════════════");
            getLogger().info("    UB-Economy Plugin Disabled Successfully");
            getLogger().info("═══════════════════════════════════════");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while disabling UB-Economy plugin!", e);
        }
    }

    /**
     * Initialize configuration manager and load configuration files
     */
    private void initializeConfiguration() {
        getLogger().info("Initializing configuration...");

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        getLogger().info("Configuration loaded successfully");
    }

    /**
     * Initialize database manager and create necessary tables
     */
    private void initializeDatabase() {
        getLogger().info("Initializing database...");

        try {
            databaseManager = new DatabaseManager(this, configManager);
            databaseManager.connect();
            databaseManager.createTables();

            getLogger().info("Database initialized successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Initialize economy manager with database and configuration
     */
    private void initializeEconomyManager() {
        getLogger().info("Initializing economy manager...");

        economyManager = new EconomyManager(this, databaseManager, configManager);

        getLogger().info("Economy manager initialized successfully");
    }

    /**
     * Register all plugin commands
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        commandManager = new CommandManager(this, economyManager);
        commandManager.registerCommands();

        getLogger().info("Commands registered successfully");
    }

    /**
     * Register all event listeners
     */
    private void registerEventListeners() {
        getLogger().info("Registering event listeners...");

        // Register player event listener
        PlayerEventListener playerEventListener = new PlayerEventListener(this, economyManager);
        Bukkit.getPluginManager().registerEvents(playerEventListener, this);
        getLogger().info("Player event listener registered");

        // Register transaction event listener
        TransactionEventListener transactionEventListener = new TransactionEventListener(this, economyManager);
        Bukkit.getPluginManager().registerEvents(transactionEventListener, this);
        getLogger().info("Transaction event listener registered");

        getLogger().info("Event listeners registered successfully");
    }

    /**
     * Load economy data from database
     */
    private void loadEconomyData() {
        getLogger().info("Loading economy data...");

        try {
            economyManager.loadAllData();
            getLogger().info("Economy data loaded successfully");

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to load economy data", e);
        }
    }

    /**
     * Get the plugin instance
     * 
     * @return UBEconomyPlugin instance
     */
    public static UBEconomyPlugin getInstance() {
        return instance;
    }

    /**
     * Get the database manager
     * 
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Get the configuration manager
     * 
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the economy manager
     * 
     * @return EconomyManager instance
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * Get the command manager
     * 
     * @return CommandManager instance
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Check if the plugin is fully initialized
     * 
     * @return true if all components are initialized
     */
    public boolean isFullyInitialized() {
        return databaseManager != null &&
                configManager != null &&
                economyManager != null &&
                commandManager != null;
    }

    /**
     * Reload plugin configuration and data
     */
    public void reloadPlugin() {
        getLogger().info("Reloading plugin configuration and data...");

        try {
            // Reload configuration
            configManager.loadConfigs();
            getLogger().info("Configuration reloaded");

            // Reload economy data
            economyManager.loadAllData();
            getLogger().info("Economy data reloaded");

            getLogger().info("Plugin reloaded successfully");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload plugin", e);
        }
    }
}
