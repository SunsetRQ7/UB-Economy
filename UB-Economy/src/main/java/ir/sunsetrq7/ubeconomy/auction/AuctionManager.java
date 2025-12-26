package ir.sunsetrq7.ubeconomy.auction;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.AuctionService;
import org.bukkit.entity.Player;

public class AuctionManager {
    
    private final UBEconomyPlugin plugin;
    private final AuctionService auctionService;
    
    public AuctionManager(UBEconomyPlugin plugin, AuctionService auctionService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
    }
    
    public void openAuctionGUI(Player player, int page) {
        AuctionGui gui = new AuctionGui(plugin, auctionService, this, page);
        gui.open(player);
    }
    
    public void openConfirmBuyGUI(Player player, int auctionId) {
        AuctionConfirmGui confirmGui = new AuctionConfirmGui(plugin, auctionService, auctionId, "buy");
        confirmGui.open(player);
    }
    
    public void openConfirmSellGUI(Player player, int auctionId) {
        AuctionConfirmGui confirmGui = new AuctionConfirmGui(plugin, auctionService, auctionId, "sell");
        confirmGui.open(player);
    }
}