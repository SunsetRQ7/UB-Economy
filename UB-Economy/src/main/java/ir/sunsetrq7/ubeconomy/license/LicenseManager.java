package ir.sunsetrq7.ubeconomy.license;

public class LicenseManager {
    
    public static boolean isLicensed() {
        // Always return true - no license enforcement
        return true;
    }
    
    public static String getLicenseStatus() {
        // Return empty string - no license enforcement
        return "Licensed";
    }
}