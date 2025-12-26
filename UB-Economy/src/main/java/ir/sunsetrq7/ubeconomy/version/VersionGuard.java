package ir.sunsetrq7.ubeconomy.version;

import org.bukkit.Bukkit;

public class VersionGuard {
    
    public static boolean isCompatible() {
        String version = Bukkit.getVersion();
        // Check if server is running 1.21.3 or higher
        return isVersionCompatible("1.21.3");
    }
    
    private static boolean isVersionCompatible(String requiredVersion) {
        try {
            String serverVersion = Bukkit.getBukkitVersion();
            
            // Extract version numbers (e.g., from "1.21.3-R0.1-SNAPSHOT" to "1.21.3")
            String cleanServerVersion = serverVersion.split("-")[0];
            
            // Extract version numbers (e.g., from "1.21.3" to [1, 21, 3])
            String[] requiredParts = requiredVersion.split("\\.");
            String[] serverParts = cleanServerVersion.split("\\.");
            
            // Pad arrays to same length
            int maxLen = Math.max(requiredParts.length, serverParts.length);
            int[] req = new int[maxLen];
            int[] serv = new int[maxLen];
            
            for (int i = 0; i < requiredParts.length; i++) {
                req[i] = Integer.parseInt(requiredParts[i]);
            }
            
            for (int i = 0; i < serverParts.length; i++) {
                serv[i] = Integer.parseInt(serverParts[i]);
            }
            
            // Compare versions
            for (int i = 0; i < maxLen; i++) {
                if (serv[i] > req[i]) {
                    return true; // Server version is higher
                } else if (serv[i] < req[i]) {
                    return false; // Server version is lower
                }
            }
            
            return true; // Versions are equal
        } catch (Exception e) {
            // If we can't parse the version, assume compatibility
            return true;
        }
    }
}