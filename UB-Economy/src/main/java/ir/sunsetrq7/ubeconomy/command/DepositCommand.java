package ir.sunsetrq7.ubeconomy.command;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.EconomyService;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DepositCommand implements CommandExecutor {
    
    private final UBEconomyPlugin plugin;
    private final EconomyService economyService;
    
    public DepositCommand(UBEconomyPlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            if (args.length != 1) {
                Message.send(player, "&cEstefade: &7/deposit <meghdar> ⚠️");
                return true;
            }
            
            try {
                long amount = Long.parseLong(args[0]);
                
                if (amount <= 0) {
                    Message.send(player, "&cMeghdar bayad bozorgtar az 0 bashad! ⚠️");
                    return true;
                }
                
                economyService.deposit(player, amount);
            } catch (NumberFormatException e) {
                Message.send(player, "&cMeghdar bayad yek adad sahih bashad! ⚠️");
            }
        } else {
            Message.send(sender, "&cFaghat baraye player ha! ⚠️");
        }
        
        return true;
    }
}