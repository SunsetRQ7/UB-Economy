package ir.sunsetrq7.ubeconomy.command;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.EconomyService;
import ir.sunsetrq7.ubeconomy.economy.EconomyAccount;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class BaltopCommand implements CommandExecutor {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public BaltopCommand(UBEconomyPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            int limit = 10; // Default to top 10
            
            // Fetch top balances asynchronously
            CompletableFuture<EconomyAccount[]> future = economyService.getTopBalances(limit);
            
            future.thenAccept(accounts -> {
                // Run on main thread to send messages
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Message.send(player, "&b&l--- Top " + limit + " Mojodi ha --- 💎 ---");
                    
                    if (accounts.length == 0) {
                        Message.send(player, "&7Hich playeri dar database mojod nist.");
                    } else {
                        for (int i = 0; i < accounts.length; i++) {
                            EconomyAccount account = accounts[i];
                            String playerName = Bukkit.getOfflinePlayer(account.getUuid()).getName();
                            if (playerName == null) {
                                playerName = "Unknown Player";
                            }
                            
                            String rank = getRank(i + 1);
                            String message = plugin.getConfig().getString("messages.baltop_entry", "&e%rank%. &a%player%: &d%balance% Diamond 💎")
                                    .replace("%rank%", rank)
                                    .replace("%player%", playerName)
                                    .replace("%balance%", String.valueOf(account.getBalance()));
                            Message.send(player, message);
                        }
                    }
                    
                    Message.send(player, "&b&l--- Payan list --- 💎 ---");
                });
            });
        } else {
            Message.send(sender, "&cFaghat baraye player ha! ⚠️");
        }
        
        return true;
    }
    
    private String getRank(int position) {
        switch (position) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return String.valueOf(position);
        }
    }
}