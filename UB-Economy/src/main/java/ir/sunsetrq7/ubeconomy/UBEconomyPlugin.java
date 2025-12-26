package ir.sunsetrq7.ubeconomy;

import ir.sunsetrq7.ubeconomy.core.ServiceRegistry;
import ir.sunsetrq7.ubeconomy.database.Database;
import ir.sunsetrq7.ubeconomy.placeholder.UBPlaceholderExpansion;
import ir.sunsetrq7.ubeconomy.version.VersionGuard;
import org.bukkit.plugin.java.JavaPlugin;

public class UBEconomyPlugin extends JavaPlugin {
    
    private Database database;
    private ServiceRegistry serviceRegistry;
    
    @Override
    public void onEnable() {
        // Check server version compatibility
        if (!VersionGuard.isCompatible()) {
            getLogger().severe("Server version is not compatible! Requires 1.21.3 or higher.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize configuration
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Initialize database
        database = new Database(this);
        database.initialize();
        
        // Initialize services
        serviceRegistry = new ServiceRegistry(this, database);
        serviceRegistry.initializeServices();
        
        // Register commands
        serviceRegistry.registerCommands();
        
        // Register events
        serviceRegistry.registerEvents();
        
        // Setup PlaceholderAPI if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UBPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled");
        }
        
        getLogger().info("UB-Economy has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Close database connection
        if (database != null) {
            database.close();
        }
        
        getLogger().info("UB-Economy has been disabled!");
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
}