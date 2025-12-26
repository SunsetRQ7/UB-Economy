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

public class PayCommand implements CommandExecutor, TabCompleter {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public PayCommand(UBEconomyPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            if (args.length != 2) {
                Message.send(player, "&cEstefade: &7/pay <player> <meghdar> ⚠️");
                return true;
            }
            
            String targetName = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                Message.send(player, "&cPlayer peyda nashod! ⚠️");
                return true;
            }
            
            UUID targetUUID = targetPlayer.getUniqueId();
            
            try {
                long amount = Long.parseLong(args[1]);
                
                if (amount <= 0) {
                    Message.send(player, "&cMeghdar bayad bozorgtar az 0 bashad! ⚠️");
                    return true;
                }
                
                economyService.pay(player, targetUUID, amount);
            } catch (NumberFormatException e) {
                Message.send(player, "&cMeghdar bayad yek adad sahih bashad! ⚠️");
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