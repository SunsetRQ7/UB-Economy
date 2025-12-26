package ir.sunsetrq7.ubeconomy.database;

import ir.sunsetrq7.ubeconomy.UBEconomyPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;

public class Database {
    private final UBEconomyPlugin plugin;
    private Connection connection;
    
    private PlayerTable playerTable;
    private AuctionTable auctionTable;
    
    public Database(UBEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        try {
            // Connect to SQLite database
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/database.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // Create tables if they don't exist
            createTables();
            
            // Initialize table handlers
            playerTable = new PlayerTable(this);
            auctionTable = new AuctionTable(this);
            
            plugin.getLogger().info("Database connected successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        try {
            // Create players table
            String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "balance INTEGER NOT NULL DEFAULT 0" +
                    ")";
            executeUpdate(createPlayersTable);
            
            // Create auctions table
            String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid TEXT, " +
                    "item_data TEXT, " +
                    "price INTEGER, " +
                    "end_time BIGINT" +
                    ")";
            executeUpdate(createAuctionsTable);
            
            plugin.getLogger().info("Database tables created/verified!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public PlayerTable getPlayerTable() {
        return playerTable;
    }
    
    public AuctionTable getAuctionTable() {
        return auctionTable;
    }
}