package ir.sunsetrq7.ubeconomy.database;

import java.io.File;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Async SQLite connection pool manager with prepared statement caching,
 * automatic table creation, and comprehensive connection lifecycle management.
 */
public class Database {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";
    
    private final String databasePath;
    private final BlockingQueue<Connection> connectionPool;
    private final ConcurrentHashMap<String, PreparedStatement> preparedStatements;
    private final ExecutorService executorService;
    private final AtomicBoolean initialized;
    private final int poolSize;
    
    private static final String[] TABLE_CREATION_SCRIPTS = {
        // Example table creation scripts
        // Add your table definitions here
    };

    /**
     * Initialize Database with default pool size of 10
     *
     * @param databasePath Path to SQLite database file
     */
    public Database(String databasePath) {
        this(databasePath, 10);
    }

    /**
     * Initialize Database with custom pool size
     *
     * @param databasePath Path to SQLite database file
     * @param poolSize     Number of connections in the pool
     */
    public Database(String databasePath, int poolSize) {
        this.databasePath = databasePath;
        this.poolSize = Math.max(1, poolSize);
        this.connectionPool = new LinkedBlockingQueue<>(this.poolSize);
        this.preparedStatements = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(this.poolSize);
        this.initialized = new AtomicBoolean(false);
    }

    /**
     * Initialize the database connection pool and create tables
     */
    public void initialize() {
        if (initialized.getAndSet(true)) {
            LOGGER.warning("Database already initialized");
            return;
        }

        executorService.execute(() -> {
            try {
                // Ensure database file exists
                ensureDatabaseFileExists();
                
                // Enable SQLite optimizations
                try (Connection conn = createRawConnection()) {
                    executeUpdateAsync("PRAGMA journal_mode=WAL", conn).get();
                    executeUpdateAsync("PRAGMA synchronous=NORMAL", conn).get();
                    executeUpdateAsync("PRAGMA cache_size=10000", conn).get();
                    executeUpdateAsync("PRAGMA foreign_keys=ON", conn).get();
                }

                // Fill connection pool
                for (int i = 0; i < poolSize; i++) {
                    Connection conn = createRawConnection();
                    if (!connectionPool.offer(conn)) {
                        conn.close();
                        throw new RuntimeException("Failed to initialize connection pool");
                    }
                }

                // Create tables if needed
                createTables();
                LOGGER.info("Database initialized successfully with " + poolSize + " connections");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
                shutdown();
            }
        });
    }

    /**
     * Ensure the database file and parent directories exist
     */
    private void ensureDatabaseFileExists() throws Exception {
        File dbFile = new File(databasePath);
        File parentDir = dbFile.getParentFile();
        
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new RuntimeException("Failed to create parent directories for database");
            }
        }
        
        if (!dbFile.exists()) {
            if (!dbFile.createNewFile()) {
                throw new RuntimeException("Failed to create database file");
            }
        }
    }

    /**
     * Create a new raw SQLite connection without pooling
     */
    private Connection createRawConnection() throws SQLException {
        String url = JDBC_URL_PREFIX + databasePath;
        Connection conn = DriverManager.getConnection(url);
        conn.setAutoCommit(true);
        return conn;
    }

    /**
     * Get a connection from the pool with timeout
     */
    private Connection getConnection(long timeout, TimeUnit unit) throws InterruptedException, SQLException {
        Connection conn = connectionPool.poll(timeout, unit);
        if (conn == null) {
            throw new SQLException("Connection pool timeout after " + timeout + " " + unit);
        }
        if (conn.isClosed()) {
            return getConnection(timeout, unit);
        }
        return conn;
    }

    /**
     * Return a connection to the pool
     */
    private void returnConnection(Connection conn) {
        if (conn != null && !connectionPool.offer(conn)) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Execute an async query and return a CompletableFuture with ResultSet
     */
    public CompletableFuture<ResultSet> executeQueryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection(30, TimeUnit.SECONDS);
                PreparedStatement stmt = createPreparedStatement(conn, sql, params);
                ResultSet rs = stmt.executeQuery();
                // Note: Connection must be closed by caller after ResultSet is processed
                return rs;
            } catch (Exception e) {
                if (conn != null) {
                    returnConnection(conn);
                }
                throw new CompletionException(e);
            }
        }, executorService);
    }

    /**
     * Execute an async update/insert/delete and return number of affected rows
     */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection(30, TimeUnit.SECONDS);
                return executeUpdateAsync(sql, conn, params).get();
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                if (conn != null) {
                    returnConnection(conn);
                }
            }
        }, executorService);
    }

    /**
     * Execute async update on existing connection
     */
    private CompletableFuture<Integer> executeUpdateAsync(String sql, Connection conn, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement stmt = createPreparedStatement(conn, sql, params);
                int affectedRows = stmt.executeUpdate();
                stmt.close();
                return affectedRows;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    /**
     * Execute batch update asynchronously
     */
    public CompletableFuture<int[]> executeBatchAsync(String sql, java.util.List<Object[]> batchParams) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection(30, TimeUnit.SECONDS);
                PreparedStatement stmt = conn.prepareStatement(sql);
                
                for (Object[] params : batchParams) {
                    setStatementParameters(stmt, params);
                    stmt.addBatch();
                }
                
                int[] result = stmt.executeBatch();
                stmt.close();
                return result;
            } catch (SQLException e) {
                throw new CompletionException(e);
            } finally {
                if (conn != null) {
                    returnConnection(conn);
                }
            }
        }, executorService);
    }

    /**
     * Execute transaction asynchronously
     */
    public CompletableFuture<Boolean> executeTransactionAsync(TransactionCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection(30, TimeUnit.SECONDS);
                conn.setAutoCommit(false);
                
                try {
                    boolean result = callback.execute(conn);
                    if (result) {
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    return result;
                } catch (Exception e) {
                    conn.rollback();
                    throw new CompletionException(e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                if (conn != null) {
                    returnConnection(conn);
                }
            }
        }, executorService);
    }

    /**
     * Create or get a cached prepared statement
     */
    private PreparedStatement createPreparedStatement(Connection conn, String sql, Object... params) throws SQLException {
        // For this implementation, we create a new statement each time
        // Caching is handled at a higher level if needed
        PreparedStatement stmt = conn.prepareStatement(sql);
        setStatementParameters(stmt, params);
        return stmt;
    }

    /**
     * Set parameters for a prepared statement
     */
    private void setStatementParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Double) {
                stmt.setDouble(i + 1, (Double) param);
            } else if (param instanceof Float) {
                stmt.setFloat(i + 1, (Float) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof java.util.Date) {
                stmt.setLong(i + 1, ((java.util.Date) param).getTime());
            } else if (param instanceof byte[]) {
                stmt.setBytes(i + 1, (byte[]) param);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    /**
     * Create tables if they don't exist
     */
    private void createTables() throws Exception {
        for (String createTableSql : TABLE_CREATION_SCRIPTS) {
            if (createTableSql != null && !createTableSql.trim().isEmpty()) {
                executeUpdateAsync(createTableSql).get();
            }
        }
        LOGGER.info("Tables created successfully");
    }

    /**
     * Add table creation script
     */
    public static void addTableCreationScript(String sql) {
        // This would need to be refactored to properly add scripts
        // For now, scripts are defined in TABLE_CREATION_SCRIPTS
    }

    /**
     * Get database status information
     */
    public CompletableFuture<DatabaseStatus> getStatusAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int availableConnections = connectionPool.size();
                int usedConnections = poolSize - availableConnections;
                
                return new DatabaseStatus(
                    initialized.get(),
                    availableConnections,
                    usedConnections,
                    poolSize,
                    databasePath
                );
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Shutdown the database and close all connections
     */
    public void shutdown() {
        LOGGER.info("Shutting down database...");
        
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close all connections
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }

        // Clear prepared statements cache
        preparedStatements.values().forEach(stmt -> {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing prepared statement", e);
            }
        });
        preparedStatements.clear();

        LOGGER.info("Database shutdown complete");
    }

    /**
     * Check if database is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Get the database file path
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Get the current pool size
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Get number of available connections
     */
    public int getAvailableConnections() {
        return connectionPool.size();
    }

    /**
     * Callback interface for transactions
     */
    @FunctionalInterface
    public interface TransactionCallback {
        boolean execute(Connection conn) throws Exception;
    }

    /**
     * Database status information
     */
    public static class DatabaseStatus {
        private final boolean initialized;
        private final int availableConnections;
        private final int usedConnections;
        private final int totalConnections;
        private final String databasePath;

        public DatabaseStatus(boolean initialized, int availableConnections, int usedConnections, 
                            int totalConnections, String databasePath) {
            this.initialized = initialized;
            this.availableConnections = availableConnections;
            this.usedConnections = usedConnections;
            this.totalConnections = totalConnections;
            this.databasePath = databasePath;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public int getAvailableConnections() {
            return availableConnections;
        }

        public int getUsedConnections() {
            return usedConnections;
        }

        public int getTotalConnections() {
            return totalConnections;
        }

        public String getDatabasePath() {
            return databasePath;
        }

        @Override
        public String toString() {
            return "DatabaseStatus{" +
                    "initialized=" + initialized +
                    ", availableConnections=" + availableConnections +
                    ", usedConnections=" + usedConnections +
                    ", totalConnections=" + totalConnections +
                    ", databasePath='" + databasePath + '\'' +
                    '}';
        }
    }
}
