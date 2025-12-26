package ir.sunsetrq7.ubeconomy.license;

public class LicenseValidator {
    
    public static boolean validate() {
        // Always return true - no license enforcement
        return true;
    }
    
    public static String getValidationMessage() {
        // Return empty string - no license enforcement
        return "";
    }
}