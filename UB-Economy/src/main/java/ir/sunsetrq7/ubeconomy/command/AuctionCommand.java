package ir.sunsetrq7.ubeconomy.command;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.auction.AuctionManager;
import ir.sunsetrq7.ubeconomy.core.AuctionService;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionCommand implements CommandExecutor {
    
    private final UBEconomyPlugin plugin;
    private final AuctionService auctionService;
    private final AuctionManager auctionManager;
    
    public AuctionCommand(UBEconomyPlugin plugin, AuctionService auctionService, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.auctionManager = auctionManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            if (args.length == 0) {
                // Open auction GUI
                auctionManager.openAuctionGUI(player, 1);
                return true;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
                try {
                    long price = Long.parseLong(args[1]);
                    
                    if (price <= 0) {
                        Message.send(player, "&cGheymat bayad bozorgtar az 0 bashad! ⚠️");
                        return true;
                    }
                    
                    // Create auction with item in hand
                    if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().isAir()) {
                        Message.send(player, "&cShoma bayad itemi dar dast-e khod dashte bashid! ⚠️");
                        return true;
                    }
                    
                    auctionService.createAuction(player, player.getInventory().getItemInMainHand().clone(), price);
                } catch (NumberFormatException e) {
                    Message.send(player, "&cGheymat bayad yek adad sahih bashad! ⚠️");
                }
                return true;
            } else {
                Message.send(player, "&cEstefade: &7/ah &8[&7sell <gheymat>&8] ⚠️");
                return true;
            }
        } else {
            Message.send(sender, "&cFaghat baraye player ha! ⚠️");
        }
        
        return true;
    }
}