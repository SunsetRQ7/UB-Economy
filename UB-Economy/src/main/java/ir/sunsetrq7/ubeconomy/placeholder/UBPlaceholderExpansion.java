package ir.sunsetrq7.ubeconomy.placeholder;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.EconomyService;
import ir.sunsetrq7.ubeconomy.economy.EconomyAccount;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;

public class UBPlaceholderExpansion extends PlaceholderExpansion {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public UBPlaceholderExpansion(UBEconomyPlugin plugin) {
        this.plugin = plugin;
        this.economyService = plugin.getServiceRegistry().getEconomyService();
    }
    
    @Override
    public String getIdentifier() {
        return "diamondeconomy";
    }
    
    @Override
    public String getAuthor() {
        return "SunsetRQ7";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return null;
        }
        
        switch (identifier.toLowerCase()) {
            case "balance":
                // Return player's balance
                return economyService.getBalance(player.getUniqueId()).join() + "";
                
            case "rank":
                // Get player's rank based on balance
                long playerBalance = economyService.getBalance(player.getUniqueId()).join();
                
                // Get top balances to determine rank
                CompletableFuture<EconomyAccount[]> future = economyService.getTopBalances(100); // Get top 100 to determine rank
                EconomyAccount[] accounts = future.join();
                
                for (int i = 0; i < accounts.length; i++) {
                    if (accounts[i].getUuid().equals(player.getUniqueId())) {
                        return String.valueOf(i + 1);
                    }
                }
                
                // If not in top 100, calculate by comparing with all balances
                int rank = 1;
                for (EconomyAccount account : accounts) {
                    if (account.getBalance() > playerBalance) {
                        rank++;
                    }
                }
                
                return String.valueOf(rank);
                
            default:
                return null;
        }
    }
}