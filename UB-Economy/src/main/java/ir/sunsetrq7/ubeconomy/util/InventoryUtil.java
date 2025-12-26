package ir.sunsetrq7.ubeconomy.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {
    
    public static boolean hasSpaceFor(Inventory inventory, ItemStack item) {
        int needed = item.getAmount();
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                // Found empty slot
                needed -= item.getMaxStackSize();
                if (needed <= 0) {
                    return true;
                }
            } else if (slot.isSimilar(item)) {
                // Found stackable slot
                int canAdd = Math.min(item.getMaxStackSize() - slot.getAmount(), needed);
                needed -= canAdd;
                if (needed <= 0) {
                    return true;
                }
            }
        }
        
        return needed <= 0;
    }
    
    public static void removeItem(Inventory inventory, Material material, int amount) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null && slot.getType() == material) {
                if (slot.getAmount() <= amount) {
                    // Remove entire stack
                    amount -= slot.getAmount();
                    inventory.setItem(i, null);
                } else {
                    // Remove partial stack
                    slot.setAmount(slot.getAmount() - amount);
                    amount = 0;
                }
                
                if (amount <= 0) {
                    break;
                }
            }
        }
        inventory.close();
    }
}