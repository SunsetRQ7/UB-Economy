package ir.sunsetrq7.ubeconomy.database;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PlayerTable class for CRUD operations on player balance data.
 * Provides async support for database operations using prepared statements.
 * 
 * @author SunsetRQ7
 * @version 1.0
 */
public class PlayerTable {
    
    private static final Logger LOGGER = Logger.getLogger(PlayerTable.class.getName());
    private static final Executor ASYNC_EXECUTOR = ForkJoinPool.commonPool();
    
    private final DatabaseConnection dbConnection;
    
    // SQL Queries
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS player_balance (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "uuid VARCHAR(36) UNIQUE NOT NULL, " +
            "username VARCHAR(16) NOT NULL, " +
            "balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00, " +
            "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";
    
    private static final String INSERT_PLAYER_SQL = "INSERT INTO player_balance (uuid, username, balance) VALUES (?, ?, ?)";
    private static final String SELECT_PLAYER_BY_UUID_SQL = "SELECT id, uuid, username, balance, last_updated FROM player_balance WHERE uuid = ?";
    private static final String SELECT_PLAYER_BY_USERNAME_SQL = "SELECT id, uuid, username, balance, last_updated FROM player_balance WHERE username = ?";
    private static final String UPDATE_BALANCE_SQL = "UPDATE player_balance SET balance = ? WHERE uuid = ?";
    private static final String ADD_BALANCE_SQL = "UPDATE player_balance SET balance = balance + ? WHERE uuid = ?";
    private static final String SUBTRACT_BALANCE_SQL = "UPDATE player_balance SET balance = balance - ? WHERE uuid = ?";
    private static final String DELETE_PLAYER_SQL = "DELETE FROM player_balance WHERE uuid = ?";
    private static final String PLAYER_EXISTS_SQL = "SELECT 1 FROM player_balance WHERE uuid = ? LIMIT 1";
    
    /**
     * Constructor for PlayerTable
     * 
     * @param dbConnection the database connection instance
     */
    public PlayerTable(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }
    
    /**
     * Creates the player_balance table if it doesn't exist
     * 
     * @return CompletableFuture that completes when table is created
     */
    public CompletableFuture<Void> createTable() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(CREATE_TABLE_SQL);
                LOGGER.info("Player balance table created or already exists");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error creating player balance table", e);
                throw new RuntimeException("Failed to create player balance table", e);
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Creates a new player record in the database
     * 
     * @param uuid the player's UUID
     * @param username the player's username
     * @param initialBalance the initial balance amount
     * @return CompletableFuture that completes with the result
     */
    public CompletableFuture<Boolean> createPlayer(UUID uuid, String username, double initialBalance) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(INSERT_PLAYER_SQL)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, username);
                pstmt.setDouble(3, initialBalance);
                int result = pstmt.executeUpdate();
                LOGGER.info("Player created: " + username + " (" + uuid + ")");
                return result > 0;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062) { // Duplicate entry
                    LOGGER.warning("Player already exists: " + username);
                } else {
                    LOGGER.log(Level.SEVERE, "Error creating player", e);
                }
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Retrieves a player's balance data by UUID
     * 
     * @param uuid the player's UUID
     * @return CompletableFuture that completes with the player's balance, or null if not found
     */
    public CompletableFuture<Double> getPlayerBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(SELECT_PLAYER_BY_UUID_SQL)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving player balance for UUID: " + uuid, e);
            }
            return null;
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Retrieves a player's complete data by UUID
     * 
     * @param uuid the player's UUID
     * @return CompletableFuture that completes with a PlayerData object, or null if not found
     */
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(SELECT_PLAYER_BY_UUID_SQL)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerData(
                                rs.getInt("id"),
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getDouble("balance"),
                                rs.getTimestamp("last_updated")
                        );
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving player data for UUID: " + uuid, e);
            }
            return null;
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Retrieves a player's balance by username
     * 
     * @param username the player's username
     * @return CompletableFuture that completes with the player's balance, or null if not found
     */
    public CompletableFuture<Double> getPlayerBalanceByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(SELECT_PLAYER_BY_USERNAME_SQL)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving player balance for username: " + username, e);
            }
            return null;
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Updates a player's balance to a specific amount
     * 
     * @param uuid the player's UUID
     * @param newBalance the new balance amount
     * @return CompletableFuture that completes with the result
     */
    public CompletableFuture<Boolean> setPlayerBalance(UUID uuid, double newBalance) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(UPDATE_BALANCE_SQL)) {
                pstmt.setDouble(1, Math.max(0, newBalance));
                pstmt.setString(2, uuid.toString());
                int result = pstmt.executeUpdate();
                LOGGER.info("Balance updated for player: " + uuid + " to: " + newBalance);
                return result > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error updating player balance", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Adds an amount to a player's balance
     * 
     * @param uuid the player's UUID
     * @param amount the amount to add
     * @return CompletableFuture that completes with the result
     */
    public CompletableFuture<Boolean> addBalance(UUID uuid, double amount) {
        if (amount < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Amount cannot be negative"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(ADD_BALANCE_SQL)) {
                pstmt.setDouble(1, amount);
                pstmt.setString(2, uuid.toString());
                int result = pstmt.executeUpdate();
                LOGGER.info("Added " + amount + " to player: " + uuid);
                return result > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error adding balance to player", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Subtracts an amount from a player's balance
     * 
     * @param uuid the player's UUID
     * @param amount the amount to subtract
     * @return CompletableFuture that completes with the result
     */
    public CompletableFuture<Boolean> subtractBalance(UUID uuid, double amount) {
        if (amount < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Amount cannot be negative"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(SUBTRACT_BALANCE_SQL)) {
                pstmt.setDouble(1, amount);
                pstmt.setString(2, uuid.toString());
                int result = pstmt.executeUpdate();
                LOGGER.info("Subtracted " + amount + " from player: " + uuid);
                return result > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error subtracting balance from player", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Checks if a player exists in the database
     * 
     * @param uuid the player's UUID
     * @return CompletableFuture that completes with true if player exists, false otherwise
     */
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(PLAYER_EXISTS_SQL)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error checking if player exists", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Deletes a player record from the database
     * 
     * @param uuid the player's UUID
     * @return CompletableFuture that completes with the result
     */
    public CompletableFuture<Boolean> deletePlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(DELETE_PLAYER_SQL)) {
                pstmt.setString(1, uuid.toString());
                int result = pstmt.executeUpdate();
                LOGGER.info("Player deleted: " + uuid);
                return result > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error deleting player", e);
                return false;
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Inner class representing player data
     */
    public static class PlayerData {
        private final int id;
        private final UUID uuid;
        private final String username;
        private final double balance;
        private final Timestamp lastUpdated;
        
        public PlayerData(int id, UUID uuid, String username, double balance, Timestamp lastUpdated) {
            this.id = id;
            this.uuid = uuid;
            this.username = username;
            this.balance = balance;
            this.lastUpdated = lastUpdated;
        }
        
        public int getId() {
            return id;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getUsername() {
            return username;
        }
        
        public double getBalance() {
            return balance;
        }
        
        public Timestamp getLastUpdated() {
            return lastUpdated;
        }
        
        @Override
        public String toString() {
            return "PlayerData{" +
                    "id=" + id +
                    ", uuid=" + uuid +
                    ", username='" + username + '\'' +
                    ", balance=" + balance +
                    ", lastUpdated=" + lastUpdated +
                    '}';
        }
    }
}
