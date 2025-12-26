package ir.sunsetrq7.ubeconomy.database;

import ir.sunsetrq7.ubeconomy.economy.EconomyAccount;

import java.sql.*;
import java.util.UUID;

public class PlayerTable {
    private final Database database;
    
    public PlayerTable(Database database) {
        this.database = database;
    }
    
    public long getBalance(UUID playerUUID) {
        String sql = "SELECT balance FROM players WHERE uuid = ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("balance");
                } else {
                    // Player doesn't exist, create with 0 balance
                    createPlayer(playerUUID, 0);
                    return 0;
                }
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting balance: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    public boolean setBalance(UUID playerUUID, long balance) {
        String sql = "INSERT OR REPLACE INTO players (uuid, balance) VALUES (?, ?)";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setLong(2, Math.max(0, balance));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error setting balance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean addBalance(UUID playerUUID, long amount) {
        if (amount < 0) return false;
        
        long currentBalance = getBalance(playerUUID);
        return setBalance(playerUUID, currentBalance + amount);
    }
    
    public boolean removeBalance(UUID playerUUID, long amount) {
        if (amount < 0) return false;
        
        long currentBalance = getBalance(playerUUID);
        if (currentBalance < amount) return false; // Not enough balance
        
        return setBalance(playerUUID, currentBalance - amount);
    }
    
    public boolean hasBalance(UUID playerUUID, long amount) {
        return getBalance(playerUUID) >= amount;
    }
    
    private void createPlayer(UUID playerUUID, long initialBalance) {
        String sql = "INSERT OR IGNORE INTO players (uuid, balance) VALUES (?, ?)";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setLong(2, initialBalance);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error creating player: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public EconomyAccount[] getTopBalances(int limit) {
        String sql = "SELECT uuid, balance FROM players ORDER BY balance DESC LIMIT ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                // Count results first
                int count = 0;
                ResultSet countRs = stmt.executeQuery();
                while (countRs.next()) count++;
                countRs.close();
                
                EconomyAccount[] accounts = new EconomyAccount[count];
                int index = 0;
                
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    long balance = rs.getLong("balance");
                    accounts[index++] = new EconomyAccount(uuid, balance);
                }
                
                return accounts;
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting top balances: " + e.getMessage());
            e.printStackTrace();
            return new EconomyAccount[0];
        }
    }
}