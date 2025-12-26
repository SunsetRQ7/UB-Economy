package ir.sunsetrq7.ubeconomy.auction;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.core.AuctionService;
import ir.sunsetrq7.ubeconomy.gui.GuiBuilder;
import ir.sunsetrq7.ubeconomy.util.ItemSerializer;
import ir.sunsetrq7.ubeconomy.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuctionGui {
    
    private final UBEconomyPlugin plugin;
    private final AuctionService auctionService;
    private final AuctionManager auctionManager;
    private final int currentPage;
    private static final int ITEMS_PER_PAGE = 45; // 54 slots - 9 navigation slots
    
    public AuctionGui(UBEconomyPlugin plugin, AuctionService auctionService, AuctionManager auctionManager, int currentPage) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.auctionManager = auctionManager;
        this.currentPage = currentPage;
    }
    
    public void open(Player player) {
        // Create GUI asynchronously to avoid blocking the main thread
        CompletableFuture<Inventory> future = CompletableFuture.supplyAsync(() -> {
            String title = plugin.getConfig().getString("messages.ah_gui_title", "&b&lAuction House - Safhe &6%page%")
                    .replace("%page%", String.valueOf(currentPage));
            
            Inventory inventory = Bukkit.createInventory(null, 54, title);
            
            // Fetch auctions for current page
            List<AuctionItem> auctions = auctionService.getAuctions(currentPage, ITEMS_PER_PAGE).join();
            
            // Fill auction items
            int slot = 0;
            for (AuctionItem auction : auctions) {
                if (slot >= 45) break; // Only fill first 45 slots
                
                ItemStack item = ItemSerializer.deserialize(auction.getItemData());
                if (item != null && item.getType() != Material.AIR) {
                    // Add auction info to item lore
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        if (lore == null) lore = new ArrayList<>();
                        
                        String sellerName = Bukkit.getOfflinePlayer(auction.getSellerUUID()).getName();
                        if (sellerName == null) sellerName = "Unknown";
                        
                        String timeRemaining = TimeUtil.formatDuration(auction.getEndTime() - System.currentTimeMillis());
                        
                        String loreTemplate = plugin.getConfig().getString("messages.ah_item_lore", 
                                "&7Forushande: &f%player%\n&6Gheymat: &e%price% Diamond 💎\n&cZaman baghi mande: &e%time_remaining% ⏳");
                        
                        lore.add("");
                        lore.add(loreTemplate
                                .replace("%player%", sellerName)
                                .replace("%price%", String.valueOf(auction.getPrice()))
                                .replace("%time_remaining%", timeRemaining));
                        
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    
                    inventory.setItem(slot++, item);
                }
            }
            
            // Add navigation items
            addNavigationItems(inventory, currentPage);
            
            return inventory;
        });
        
        // Open inventory on main thread when async task completes
        future.thenAccept(inventory -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(inventory);
            });
        });
    }
    
    private void addNavigationItems(Inventory inventory, int currentPage) {
        // Previous page button (red wool)
        ItemStack prevPage = new ItemStack(Material.RED_WOOL);
        ItemMeta prevMeta = prevPage.getItemMeta();
        prevMeta.setDisplayName(plugin.getConfig().getString("messages.ah_prev_page", "&cصفحه قبلی"));
        prevPage.setItemMeta(prevMeta);
        
        // Next page button (green wool)
        ItemStack nextPage = new ItemStack(Material.GREEN_WOOL);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName(plugin.getConfig().getString("messages.ah_next_page", "&aصفحه بعدی"));
        nextPage.setItemMeta(nextMeta);
        
        // Sell button (spyglass)
        ItemStack sellButton = new ItemStack(Material.SPYGLASS);
        ItemMeta sellMeta = sellButton.getItemMeta();
        sellMeta.setDisplayName(plugin.getConfig().getString("messages.ah_sell_button", "&6&lForush Item"));
        List<String> sellLore = new ArrayList<>();
        sellLore.add(plugin.getConfig().getString("messages.ah_sell_lore", "&7Forush item-e dast-e shoma"));
        sellMeta.setLore(sellLore);
        sellButton.setItemMeta(sellMeta);
        
        // Sort buttons
        ItemStack sortNewest = new ItemStack(Material.CLOCK);
        ItemMeta sortNewestMeta = sortNewest.getItemMeta();
        sortNewestMeta.setDisplayName(plugin.getConfig().getString("messages.ah_sort_newest", "&e&lTazeh Tarin"));
        sortNewest.setItemMeta(sortNewestMeta);
        
        ItemStack sortPriceLow = new ItemStack(Material.COMPARATOR);
        ItemMeta sortPriceLowMeta = sortPriceLow.getItemMeta();
        sortPriceLowMeta.setDisplayName(plugin.getConfig().getString("messages.ah_sort_price_low", "&e&lGheymat (Kam > Ziad)"));
        sortPriceLow.setItemMeta(sortPriceLowMeta);
        
        ItemStack sortPriceHigh = new ItemStack(Material.REPEATER);
        ItemMeta sortPriceHighMeta = sortPriceHigh.getItemMeta();
        sortPriceHighMeta.setDisplayName(plugin.getConfig().getString("messages.ah_sort_price_high", "&e&lGheymat (Ziad > Kam)"));
        sortPriceHigh.setItemMeta(sortPriceHighMeta);
        
        // Fill bottom row with navigation items
        inventory.setItem(45, prevPage);      // Previous page
        inventory.setItem(46, sortNewest);    // Sort by newest
        inventory.setItem(47, sortPriceLow);  // Sort by price low to high
        inventory.setItem(48, sortPriceHigh); // Sort by price high to low
        inventory.setItem(49, sellButton);    // Sell button (center)
        inventory.setItem(50, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); // Empty
        inventory.setItem(51, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); // Empty
        inventory.setItem(52, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); // Empty
        inventory.setItem(53, nextPage);      // Next page
        
        // Fill empty slots with gray glass panes
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }
}