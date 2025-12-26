package ir.sunsetrq7.ubeconomy.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class Message {
    
    public static String colorize(String message) {
        if (message == null) return null;
        
        // Replace & with § for color codes
        Pattern pattern = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
    
    public static void send(Player player, String message) {
        player.sendMessage(colorize(message));
    }
}