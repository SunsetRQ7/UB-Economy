package ir.sunsetrq7.ubeconomy.economy;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class VaultEconomyProvider implements Economy {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public VaultEconomyProvider(UBEconomyPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }
    
    public void register() {
        plugin.getServer().getServicesManager().register(Economy.class, this, plugin, org.bukkit.plugin.ServicePriority.Normal);
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public String getName() {
        return "UB-Economy";
    }
    
    @Override
    public boolean hasBankSupport() {
        return false;
    }
    
    @Override
    public int fractionalDigits() {
        return 0;
    }
    
    @Override
    public String format(double amount) {
        return ((long) amount) + " Diamond";
    }
    
    @Override
    public String currencyNamePlural() {
        return "Diamonds";
    }
    
    @Override
    public String currencyNameSingular() {
        return "Diamond";
    }
    
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true; // All players have accounts by default
    }
    
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
    
    @Override
    public double getBalance(OfflinePlayer player) {
        return economyService.getBalance(player.getUniqueId()).join();
    }
    
    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }
    
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economyService.hasBalance(player.getUniqueId(), (long) amount).join();
    }
    
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        boolean success = economyService.removeBalance(player.getUniqueId(), (long) amount).join();
        return new EconomyResponse(amount, getBalance(player), 
                success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                success ? null : "Insufficient balance or other error");
    }
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        boolean success = economyService.addBalance(player.getUniqueId(), (long) amount).join();
        return new EconomyResponse(amount, getBalance(player),
                success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                success ? null : "Deposit error");
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankHas( String name, double amount ) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public List<String> getBanks() {
        return java.util.Collections.emptyList();
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true; // Accounts are created automatically
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}