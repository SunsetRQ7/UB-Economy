package ir.sunsetrq7.ubeconomy.util;

public class SecurityUtil {
    
    public static boolean isValidNumber(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidPlayerName(String input) {
        if (input == null || input.length() < 1 || input.length() > 16) {
            return false;
        }
        
        // Check if name contains only valid characters (alphanumeric and underscore)
        return input.matches("^[a-zA-Z0-9_]+$");
    }
}