package ir.sunsetrq7.ubeconomy.util;

public class TimeUtil {
    
    public static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "0 daghighe";
        }
        
        long totalSeconds = milliseconds / 1000;
        long days = totalSeconds / (24 * 3600);
        totalSeconds %= (24 * 3600);
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append(" rooz");
            if (hours > 0 || minutes > 0) {
                result.append(" o ");
            }
        }
        
        if (hours > 0) {
            result.append(hours).append(" saat");
            if (minutes > 0) {
                result.append(" o ");
            }
        }
        
        if (minutes > 0 || (days == 0 && hours == 0)) { // Always show minutes if days and hours are 0
            result.append(minutes).append(" daghighe");
        }
        
        return result.toString();
    }
}