package ir.sunsetrq7.ubeconomy.auction;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.AuctionService;
import ir.sunsetrq7.ubeconomy.util.ItemSerializer;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AuctionConfirmGui {
    
    private final UBEconomyPlugin plugin;
    private final AuctionService auctionService;
    private final int auctionId;
    private final String action; // "buy" or "sell"
    
    public AuctionConfirmGui(UBEconomyPlugin plugin, AuctionService auctionService, int auctionId, String action) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.auctionId = auctionId;
        this.action = action;
    }
    
    public void open(Player player) {
        String title = plugin.getConfig().getString("messages.ah_confirm_title", "&c&lTaeed " + (action.equals("buy") ? "Khared" : "Forush"));
        Inventory inventory = Bukkit.createInventory(null, 9, title);
        
        AuctionItem auction = auctionService.getAuction(auctionId);
        if (auction == null) {
            Message.send(player, "&cAuction peyda nashod! ⚠️");
            return;
        }
        
        // Get the item from auction
        ItemStack item = ItemSerializer.deserialize(auction.getItemData());
        if (item == null || item.getType() == Material.AIR) {
            Message.send(player, "&cKhataye system! Item gheyr mojod ast... ⚠️");
            return;
        }
        
        // Add item to center with confirmation info
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String sellerName = Bukkit.getOfflinePlayer(auction.getSellerUUID()).getName();
            if (sellerName == null) sellerName = "Unknown";
            
            String actionText = action.equals("buy") ? "Khared" : "Forush";
            String actionPrice = action.equals("buy") ? "Gheymat Khared:" : "Gheymat Forush:";
            
            meta.setLore(Arrays.asList(
                " ",
                "&7" + actionText + ": &e" + auction.getPrice() + " Diamond 💎",
                "&7" + actionPrice + " &e" + auction.getPrice() + " Diamond 💎",
                "&7Forushande: &f" + sellerName,
                " ",
                "&aClick konid baraye taeed",
                "&cClick raast baraye cancel"
            ));
            item.setItemMeta(meta);
        }
        
        inventory.setItem(4, item);
        
        // Yes button (green wool)
        ItemStack yesButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta yesMeta = yesButton.getItemMeta();
        yesMeta.setDisplayName("&aTaeed");
        yesMeta.setLore(Arrays.asList("&7Click konid baraye taeed"));
        yesButton.setItemMeta(yesMeta);
        
        // No button (red wool)
        ItemStack noButton = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = noButton.getItemMeta();
        noMeta.setDisplayName("&cCancel");
        noMeta.setLore(Arrays.asList("&7Click konid baraye cancel"));
        noButton.setItemMeta(noMeta);
        
        inventory.setItem(2, noButton);
        inventory.setItem(6, yesButton);
        
        player.openInventory(inventory);
    }
}