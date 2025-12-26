package ir.sunsetrq7.ubeconomy.core;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.auction.AuctionManager;
import ir.sunsetrq7.ubeconomy.command.*;
import ir.sunsetrq7.ubeconomy.database.Database;
import ir.sunsetrq7.ubeconomy.economy.VaultEconomyProvider;
import org.bukkit.Bukkit;

public class ServiceRegistry {
    
    private final UBEconomyPlugin plugin;
    private final Database database;
    
    private EconomyService economyService;
    private AuctionService auctionService;
    private VaultEconomyProvider vaultProvider;
    private AuctionManager auctionManager;
    
    public ServiceRegistry(UBEconomyPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    public void initializeServices() {
        // Initialize core services
        this.economyService = new EconomyService(plugin, database);
        this.auctionService = new AuctionService(plugin, database);
        this.auctionManager = new AuctionManager(plugin, auctionService);
        
        // Initialize vault provider if Vault is available
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.vaultProvider = new VaultEconomyProvider(plugin, economyService);
            this.vaultProvider.register();
        }
    }
    
    public void registerCommands() {
        // Register commands
        plugin.getCommand("balance").setExecutor(new BalanceCommand(plugin, economyService));
        plugin.getCommand("bal").setExecutor(new BalanceCommand(plugin, economyService));
        plugin.getCommand("deposit").setExecutor(new DepositCommand(plugin, economyService));
        plugin.getCommand("withdraw").setExecutor(new WithdrawCommand(plugin, economyService));
        plugin.getCommand("pay").setExecutor(new PayCommand(plugin, economyService));
        plugin.getCommand("baltop").setExecutor(new BaltopCommand(plugin, economyService));
        plugin.getCommand("ah").setExecutor(new AuctionCommand(plugin, auctionService, auctionManager));
    }
    
    public void registerEvents() {
        // Register events if needed
        // Currently no events needed but kept for future expansion
    }
    
    public EconomyService getEconomyService() {
        return economyService;
    }
    
    public AuctionService getAuctionService() {
        return auctionService;
    }
    
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
    
    public VaultEconomyProvider getVaultProvider() {
        return vaultProvider;
    }
}