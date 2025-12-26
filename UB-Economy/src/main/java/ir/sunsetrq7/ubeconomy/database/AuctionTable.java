package ir.sunsetrq7.ubeconomy.database;

import ir.sunsetrq7.ubeconomy.auction.AuctionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionTable {
    private final Database database;
    
    public AuctionTable(Database database) {
        this.database = database;
    }
    
    public boolean createAuction(UUID sellerUUID, String itemData, long price, long endTime) {
        String sql = "INSERT INTO auctions (seller_uuid, item_data, price, end_time) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sellerUUID.toString());
            stmt.setString(2, itemData);
            stmt.setLong(3, price);
            stmt.setLong(4, endTime);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error creating auction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public AuctionItem getAuction(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, auctionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    UUID sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    String itemData = rs.getString("item_data");
                    long price = rs.getLong("price");
                    long endTime = rs.getLong("end_time");
                    
                    return new AuctionItem(id, sellerUUID, itemData, price, endTime);
                }
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public List<AuctionItem> getAuctions(int offset, int limit) {
        String sql = "SELECT * FROM auctions WHERE end_time > ? ORDER BY end_time ASC LIMIT ? OFFSET ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<AuctionItem> auctions = new ArrayList<>();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    String itemData = rs.getString("item_data");
                    long price = rs.getLong("price");
                    long endTime = rs.getLong("end_time");
                    
                    auctions.add(new AuctionItem(id, sellerUUID, itemData, price, endTime));
                }
                
                return auctions;
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting auctions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public int getTotalAuctionCount() {
        String sql = "SELECT COUNT(*) FROM auctions WHERE end_time > ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting total auction count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public long getPlayerAuctionCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM auctions WHERE seller_uuid = ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting player auction count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public boolean removeAuction(int auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, auctionId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error removing auction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<AuctionItem> getExpiredAuctions() {
        String sql = "SELECT * FROM auctions WHERE end_time <= ?";
        
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<AuctionItem> expiredAuctions = new ArrayList<>();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    String itemData = rs.getString("item_data");
                    long price = rs.getLong("price");
                    long endTime = rs.getLong("end_time");
                    
                    expiredAuctions.add(new AuctionItem(id, sellerUUID, itemData, price, endTime));
                }
                
                return expiredAuctions;
            }
        } catch (SQLException e) {
            database.getPlugin().getLogger().severe("Error getting expired auctions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}