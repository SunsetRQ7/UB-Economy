package ir.sunsetrq7.ubeconomy.command;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.EconomyService;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BalanceCommand implements CommandExecutor, TabCompleter {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public BalanceCommand(UBEconomyPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            if (args.length == 0) {
                // Show own balance
                economyService.getBalance(player.getUniqueId()).thenAccept(balance -> {
                    String message = plugin.getConfig().getString("messages.balance_self", "&aShoma &d%balance%&a Diamond darid 💎")
                            .replace("%balance%", String.valueOf(balance));
                    Message.send(player, message);
                });
            } else if (args.length == 1) {
                // Show another player's balance
                String targetName = args[0];
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
                
                if (targetPlayer.hasPlayedBefore() || targetPlayer.isOnline()) {
                    UUID targetUUID = targetPlayer.getUniqueId();
                    economyService.getBalance(targetUUID).thenAccept(balance -> {
                        String message = plugin.getConfig().getString("messages.balance_other", "&aMojodi &d%player%&a: &d%balance%&a Diamond 💎")
                                .replace("%player%", targetName)
                                .replace("%balance%", String.valueOf(balance));
                        Message.send(player, message);
                    });
                } else {
                    Message.send(player, "&cPlayer peyda nashod! ⚠️");
                }
            } else {
                Message.send(player, "&cEstefade: &7/bal [player] ⚠️");
            }
        } else {
            Message.send(sender, "&cFaghat baraye player ha! ⚠️");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Provide player name suggestions
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}