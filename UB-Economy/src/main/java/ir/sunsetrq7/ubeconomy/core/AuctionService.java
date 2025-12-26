package ir.sunsetrq7.ubeconomy.core;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.auction.AuctionItem;
import ir.sunsetrq7.ubeconomy.database.Database;
import ir.sunsetrq7.ubeconomy.util.ItemSerializer;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuctionService {
    
    private final UBEconomyPlugin plugin;
    private final Database database;
    
    public AuctionService(UBEconomyPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    public boolean createAuction(Player seller, ItemStack item, long price) {
        if (seller.getGameMode().isCreative()) {
            Message.send(seller, "&cShoma dar halat Creative nemitavanid item beforushid 💎");
            return false;
        }
        
        if (price <= 0) {
            Message.send(seller, "&cGheymat bayad bozorgtar az 0 bashad! ⚠️");
            return false;
        }
        
        int maxAuctionPrice = plugin.getConfig().getInt("auction.limits.max_price", 1000000);
        if (price > maxAuctionPrice) {
            Message.send(seller, "&cGheymat az had mojaz bishtar ast! &7(&c" + price + " > " + maxAuctionPrice + "&7) ⚠️");
            return false;
        }
        
        // Check if player has max listings
        int maxListings = plugin.getConfig().getInt("auction.limits.max_listings_per_player", 10);
        long currentListings = database.getAuctionTable().getPlayerAuctionCount(seller.getUniqueId());
        if (currentListings >= maxListings) {
            Message.send(seller, "&cShoma az ghabl bishtarin tedad forush (%max%) ra dashte id! &7(&c" + currentListings + " >= " + maxListings + "&7) ⚠️"
                    .replace("%max%", String.valueOf(maxListings)));
            return false;
        }
        
        // Validate item is not blacklisted
        if (isItemBlacklisted(item)) {
            Message.send(seller, "&cIn item mojaz baraye forush nist! ⚠️");
            return false;
        }
        
        // Remove item from seller's inventory (validate they have it first)
        if (seller.getInventory().first(item.getType()) == -1) {
            Message.send(seller, "&cItem mored nazar dar inventory shoma nist! ⚠️");
            return false;
        }
        
        // Remove the item from inventory
        seller.getInventory().removeItem(item);
        seller.updateInventory();
        
        // Serialize item to store in database
        String itemData = ItemSerializer.serialize(item);
        
        // Calculate end time
        long duration = plugin.getConfig().getLong("auction.expiration_days", 7) * 24 * 60 * 60 * 1000L; // Convert days to milliseconds
        long endTime = System.currentTimeMillis() + duration;
        
        // Create auction in database
        boolean success = database.getAuctionTable().createAuction(seller.getUniqueId(), itemData, price, endTime);
        
        if (success) {
            Message.send(seller, "&aItem shoma ba movafaghiat dar auction gharar gereft! &7(&d" + price + " Diamond&7) 💎");
            return true;
        } else {
            // Rollback: add item back to inventory if database insertion failed
            seller.getInventory().addItem(item);
            Message.send(seller, "&cKhataye system! Item be inventory shoma barmigardand... ⚠️");
            return false;
        }
    }
    
    public boolean buyAuction(Player buyer, int auctionId) {
        if (buyer.getGameMode().isCreative()) {
            Message.send(buyer, "&cShoma dar halat Creative nemitavanid khared konid 💎");
            return false;
        }
        
        // Get auction info
        AuctionItem auction = database.getAuctionTable().getAuction(auctionId);
        if (auction == null) {
            Message.send(buyer, "&cAuction peyda nashod! ⚠️");
            return false;
        }
        
        // Check if auction is expired
        if (auction.isExpired()) {
            Message.send(buyer, "&cIn auction ghablan manzoor shode ast! ⚠️");
            return false;
        }
        
        // Check if buyer is the seller
        if (auction.getSellerUUID().equals(buyer.getUniqueId())) {
            Message.send(buyer, "&cShoma nemitavanid item khod ra beforushid! ⚠️");
            return false;
        }
        
        // Check if buyer has enough balance
        if (!database.getPlayerTable().hasBalance(buyer.getUniqueId(), auction.getPrice())) {
            Message.send(buyer, "&cMojodi kafi nist! &7(&c" + database.getPlayerTable().getBalance(buyer.getUniqueId()) + " < " + auction.getPrice() + "&7) ⚠️");
            return false;
        }
        
        // Deserialize item
        ItemStack item = ItemSerializer.deserialize(auction.getItemData());
        if (item == null || item.getType() == Material.AIR) {
            Message.send(buyer, "&cKhataye system! Item gheyr mojod ast... ⚠️");
            // Remove invalid auction from database
            database.getAuctionTable().removeAuction(auctionId);
            return false;
        }
        
        // Check if buyer has inventory space
        if (!hasInventorySpace(buyer, item)) {
            Message.send(buyer, "&cInventory shoma por ast! ⚠️");
            return false;
        }
        
        // Perform transaction
        boolean buyerSuccess = database.getPlayerTable().removeBalance(buyer.getUniqueId(), auction.getPrice());
        if (buyerSuccess) {
            // Add item to buyer's inventory
            buyer.getInventory().addItem(item);
            
            // Add money to seller
            String sellerName = Bukkit.getOfflinePlayer(auction.getSellerUUID()).getName();
            if (sellerName == null) {
                sellerName = "Unknown";
            }
            
            database.getPlayerTable().addBalance(auction.getSellerUUID(), auction.getPrice());
            
            // Notify buyer
            Message.send(buyer, "&aShoma item ra ba &d" + auction.getPrice() + " Diamond &akharid ✅ 💎");
            
            // Notify seller if online
            Player seller = Bukkit.getPlayer(auction.getSellerUUID());
            if (seller != null && seller.isOnline()) {
                Message.send(seller, "&aItem shoma be &d" + buyer.getName() + " &aforushte shod! &d" + auction.getPrice() + " Diamond &adr gereftid ✅ 💎");
            }
            
            // Remove auction from database
            database.getAuctionTable().removeAuction(auctionId);
            
            return true;
        } else {
            Message.send(buyer, "&cKhataye system! Khared namovafagh bud... ⚠️");
            return false;
        }
    }
    
    public CompletableFuture<List<AuctionItem>> getAuctions(int page, int itemsPerPage) {
        return CompletableFuture.supplyAsync(() -> {
            int offset = (page - 1) * itemsPerPage;
            return database.getAuctionTable().getAuctions(offset, itemsPerPage);
        });
    }
    
    public CompletableFuture<Integer> getTotalAuctionPages(int itemsPerPage) {
        return CompletableFuture.supplyAsync(() -> {
            int totalAuctions = database.getAuctionTable().getTotalAuctionCount();
            return (int) Math.ceil((double) totalAuctions / itemsPerPage);
        });
    }
    
    public boolean isItemBlacklisted(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }
        
        // Check against blacklisted materials from config
        List<String> blacklistedMaterials = plugin.getConfig().getStringList("auction.blacklisted_materials");
        for (String materialName : blacklistedMaterials) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                if (item.getType() == material) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Invalid material name in config, skip
            }
        }
        
        return false;
    }
    
    private boolean hasInventorySpace(Player player, ItemStack item) {
        // Check if player has space for the item
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                return true; // Found empty slot
            }
            if (slot.isSimilar(item) && slot.getAmount() + item.getAmount() <= slot.getMaxStackSize()) {
                return true; // Found stackable slot
            }
        }
        return false; // No space found
    }
    
    public boolean claimExpiredAuction(Player player, int auctionId) {
        AuctionItem auction = database.getAuctionTable().getAuction(auctionId);
        if (auction == null) {
            return false;
        }
        
        if (!auction.getSellerUUID().equals(player.getUniqueId())) {
            return false; // Only seller can claim expired auction
        }
        
        if (!auction.isExpired()) {
            return false; // Auction is not expired
        }
        
        // Deserialize item
        ItemStack item = ItemSerializer.deserialize(auction.getItemData());
        if (item == null || item.getType() == Material.AIR) {
            // Remove invalid auction from database
            database.getAuctionTable().removeAuction(auctionId);
            return false;
        }
        
        // Check if player has inventory space
        if (!hasInventorySpace(player, item)) {
            Message.send(player, "&cInventory shoma por ast! &7(&cItem: " + item.getType().name() + "&7) ⚠️");
            return false;
        }
        
        // Add item back to player's inventory
        player.getInventory().addItem(item);
        
        // Remove auction from database
        database.getAuctionTable().removeAuction(auctionId);
        
        Message.send(player, "&aItem ghabz shode be inventory shoma ezafe shod ✅ 💎");
        return true;
    }
}