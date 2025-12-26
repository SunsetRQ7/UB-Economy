package ir.sunsetrq7.ubeconomy.core;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import ir.sunsetrq7.ubeconomy.database.Database;
import ir.sunsetrq7.ubeconomy.economy.EconomyAccount;
import ir.sunsetrq7.ubeconomy.util.InventoryUtil;
import ir.sunsetrq7.ubeconomy.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyService {
    
    private final UBEconomyPlugin plugin;
    private final Database database;
    
    public EconomyService(UBEconomyPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }
    
    public CompletableFuture<Long> getBalance(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            return database.getPlayerTable().getBalance(playerUUID);
        });
    }
    
    public CompletableFuture<Boolean> setBalance(UUID playerUUID, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            return database.getPlayerTable().setBalance(playerUUID, Math.max(0, amount));
        });
    }
    
    public CompletableFuture<Boolean> addBalance(UUID playerUUID, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount < 0) return false;
            return database.getPlayerTable().addBalance(playerUUID, amount);
        });
    }
    
    public CompletableFuture<Boolean> removeBalance(UUID playerUUID, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount < 0) return false;
            return database.getPlayerTable().removeBalance(playerUUID, amount) && 
                   database.getPlayerTable().getBalance(playerUUID) >= 0;
        });
    }
    
    public CompletableFuture<Boolean> hasBalance(UUID playerUUID, long amount) {
        return getBalance(playerUUID).thenApply(balance -> balance >= amount);
    }
    
    public boolean deposit(Player player, long amount) {
        if (player.getGameMode().isCreative()) {
            Message.send(player, "&cShoma dar halat Creative nemitavanid Diamond darid 💎");
            return false;
        }
        
        if (amount <= 0) {
            Message.send(player, "&cMeghdar bayad bozorgtar az 0 bashad! ⚠️");
            return false;
        }
        
        int maxDeposit = plugin.getConfig().getInt("economy.limits.max_deposit", 1000000);
        if (amount > maxDeposit) {
            Message.send(player, "&cMeghdar vorudi az had mojaz bishtar ast! &7(&c" + amount + " > " + maxDeposit + "&7) ⚠️");
            return false;
        }
        
        ItemStack diamonds = new ItemStack(Material.DIAMOND, (int) Math.min(amount, Integer.MAX_VALUE));
        int availableAmount = player.getInventory().first(diamonds.getType(), diamonds.getDurability(), diamonds.getAmount());
        
        if (availableAmount < amount) {
            Message.send(player, "&cShoma Diamond kafi nadarid! &7(&c" + availableAmount + " / " + amount + "&7) ⚠️");
            return false;
        }
        
        // Remove diamonds from inventory
        InventoryUtil.removeItem(player.getInventory(), Material.DIAMOND, (int) amount);
        
        // Add to balance
        boolean success = database.getPlayerTable().addBalance(player.getUniqueId(), amount);
        
        if (success) {
            Message.send(player, "&a" + amount + " Diamond vared hesab shod ✅ &7(&d" + (database.getPlayerTable().getBalance(player.getUniqueId())) + " Diamond&7) 💎");
            return true;
        } else {
            // Rollback if failed
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, (int) amount));
            Message.send(player, "&cKhataye system! Diamond be inventory shoma barmigardand... ⚠️");
            return false;
        }
    }
    
    public boolean withdraw(Player player, long amount) {
        if (player.getGameMode().isCreative()) {
            Message.send(player, "&cShoma dar halat Creative nemitavanid Diamond bardarid 💎");
            return false;
        }
        
        if (amount <= 0) {
            Message.send(player, "&cMeghdar bayad bozorgtar az 0 bashad! ⚠️");
            return false;
        }
        
        int maxWithdraw = plugin.getConfig().getInt("economy.limits.max_withdraw", 1000000);
        if (amount > maxWithdraw) {
            Message.send(player, "&cMeghdar bardasht az had mojaz bishtar ast! &7(&c" + amount + " > " + maxWithdraw + "&7) ⚠️");
            return false;
        }
        
        if (!database.getPlayerTable().hasBalance(player.getUniqueId(), amount)) {
            Message.send(player, "&cMojodi kafi nist! &7(&c" + database.getPlayerTable().getBalance(player.getUniqueId()) + " < " + amount + "&7) ⚠️");
            return false;
        }
        
        // Check if player has enough inventory space
        if (!InventoryUtil.hasSpaceFor(player.getInventory(), new ItemStack(Material.DIAMOND, (int) Math.min(amount, Integer.MAX_VALUE)))) {
            Message.send(player, "&cInventory shoma por ast! &7(&cSpace needed: " + Math.min(amount, Integer.MAX_VALUE) + "&7) ⚠️");
            return false;
        }
        
        // Remove from balance
        boolean success = database.getPlayerTable().removeBalance(player.getUniqueId(), amount);
        
        if (success) {
            // Add diamonds to inventory
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, (int) Math.min(amount, Integer.MAX_VALUE)));
            Message.send(player, "&a" + amount + " Diamond az hesab bardasht shod ✅ &7(&d" + (database.getPlayerTable().getBalance(player.getUniqueId())) + " Diamond&7) 💎");
            return true;
        } else {
            Message.send(player, "&cKhataye system! Bardasht mojodi namovafagh bud... ⚠️");
            return false;
        }
    }
    
    public boolean pay(Player sender, UUID receiverUUID, long amount) {
        if (sender.getGameMode().isCreative()) {
            Message.send(sender, "&cShoma dar halat Creative nemitavanid Diamond enteqal dahid 💎");
            return false;
        }
        
        if (amount <= 0) {
            Message.send(sender, "&cMeghdar bayad bozorgtar az 0 bashad! ⚠️");
            return false;
        }
        
        int maxPay = plugin.getConfig().getInt("economy.limits.max_pay", 1000000);
        if (amount > maxPay) {
            Message.send(sender, "&cMeghdar enteqal az had mojaz bishtar ast! &7(&c" + amount + " > " + maxPay + "&7) ⚠️");
            return false;
        }
        
        if (!database.getPlayerTable().hasBalance(sender.getUniqueId(), amount)) {
            Message.send(sender, "&cMojodi kafi nist! &7(&c" + database.getPlayerTable().getBalance(sender.getUniqueId()) + " < " + amount + "&7) ⚠️");
            return false;
        }
        
        // Validate receiver exists
        String receiverName = Bukkit.getOfflinePlayer(receiverUUID).getName();
        if (receiverName == null) {
            Message.send(sender, "&cPlayer peyda nashod! ⚠️");
            return false;
        }
        
        // Perform transaction
        boolean senderSuccess = database.getPlayerTable().removeBalance(sender.getUniqueId(), amount);
        if (senderSuccess) {
            boolean receiverSuccess = database.getPlayerTable().addBalance(receiverUUID, amount);
            if (receiverSuccess) {
                // Notify sender
                Message.send(sender, "&a" + amount + " Diamond be &d" + receiverName + " &aenteqal yافت ✅ &7(&d" + (database.getPlayerTable().getBalance(sender.getUniqueId())) + " Diamond&7) 💎");
                
                // Notify receiver if online
                Player receiver = Bukkit.getPlayer(receiverUUID);
                if (receiver != null && receiver.isOnline()) {
                    Message.send(receiver, "&aShoma &d" + amount + " &aDiamond az &d" + sender.getName() + " &adr gereftid ✅ &7(&d" + (database.getPlayerTable().getBalance(receiverUUID)) + " Diamond&7) 💎");
                }
                return true;
            } else {
                // Rollback sender if receiver fails
                database.getPlayerTable().addBalance(sender.getUniqueId(), amount);
                Message.send(sender, "&cKhataye system! Enteqal namovafagh bud... ⚠️");
                return false;
            }
        } else {
            Message.send(sender, "&cKhataye system! Enteqal namovafagh bud... ⚠️");
            return false;
        }
    }
    
    public CompletableFuture<EconomyAccount[]> getTopBalances(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return database.getPlayerTable().getTopBalances(limit);
        });
    }
}