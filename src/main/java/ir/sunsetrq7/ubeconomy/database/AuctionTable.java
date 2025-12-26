package ir.sunsetrq7.ubeconomy.database;

import ir.sunsetrq7.ubeconomy.model.AuctionItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * AuctionTable provides CRUD operations for auction data with item serialization and async support.
 * This class handles all database operations related to auctions with thread-safe access patterns.
 */
public class AuctionTable {

    private final File dataFile;
    private final ExecutorService executorService;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<UUID, AuctionItem> auctionCache;
    private YamlConfiguration config;

    /**
     * Initialize AuctionTable with the specified data file path.
     * 
     * @param dataFile The file to store auction data
     */
    public AuctionTable(File dataFile) {
        this.dataFile = dataFile;
        this.executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "AuctionTable-Worker");
                t.setDaemon(true);
                return t;
            }
        );
        this.auctionCache = new ConcurrentHashMap<>();
        this.config = new YamlConfiguration();
        initializeDataFile();
    }

    /**
     * Initialize the data file if it doesn't exist.
     */
    private void initializeDataFile() {
        lock.writeLock().lock();
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }
            loadFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create or update an auction item.
     * 
     * @param auctionId The unique ID for the auction
     * @param item The auction item to save
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> upsertAuction(UUID auctionId, AuctionItem item) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctionCache.put(auctionId, item);
                saveToFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }, executorService);
    }

    /**
     * Retrieve an auction item by ID.
     * 
     * @param auctionId The ID of the auction to retrieve
     * @return CompletableFuture containing the auction item or empty Optional
     */
    public CompletableFuture<Optional<AuctionItem>> getAuction(UUID auctionId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return Optional.ofNullable(auctionCache.get(auctionId));
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Retrieve all auctions.
     * 
     * @return CompletableFuture containing a list of all auction items
     */
    public CompletableFuture<List<AuctionItem>> getAllAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return new ArrayList<>(auctionCache.values());
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Retrieve auctions by seller UUID.
     * 
     * @param sellerUuid The UUID of the seller
     * @return CompletableFuture containing a list of auctions by the seller
     */
    public CompletableFuture<List<AuctionItem>> getAuctionsBySeller(UUID sellerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctionCache.values().stream()
                    .filter(item -> item.getSellerUuid().equals(sellerUuid))
                    .toList();
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Retrieve active (non-expired) auctions.
     * 
     * @return CompletableFuture containing a list of active auctions
     */
    public CompletableFuture<List<AuctionItem>> getActiveAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                long currentTime = System.currentTimeMillis();
                return auctionCache.values().stream()
                    .filter(item -> item.getExpirationTime() > currentTime)
                    .toList();
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Delete an auction item by ID.
     * 
     * @param auctionId The ID of the auction to delete
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> deleteAuction(UUID auctionId) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctionCache.remove(auctionId);
                saveToFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }, executorService);
    }

    /**
     * Delete multiple auctions by ID.
     * 
     * @param auctionIds The IDs of the auctions to delete
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> deleteAuctions(Collection<UUID> auctionIds) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctionIds.forEach(auctionCache::remove);
                saveToFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }, executorService);
    }

    /**
     * Clear all auctions from the database.
     * 
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> clearAllAuctions() {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctionCache.clear();
                saveToFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }, executorService);
    }

    /**
     * Get the count of total auctions.
     * 
     * @return CompletableFuture containing the count
     */
    public CompletableFuture<Integer> getAuctionCount() {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctionCache.size();
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Reload auctions from file.
     * 
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> reload() {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                loadFromFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }, executorService);
    }

    /**
     * Save auctions to file.
     * 
     * @return CompletableFuture that completes when the operation is done
     */
    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> {
            lock.readLock().lock();
            try {
                saveToFile();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.readLock().unlock();
            }
        }, executorService);
    }

    /**
     * Load auctions from the configuration file.
     * This method should be called within a write lock.
     */
    private void loadFromFile() throws IOException {
        auctionCache.clear();

        if (!dataFile.exists()) {
            return;
        }

        config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection auctionsSection = config.getConfigurationSection("auctions");

        if (auctionsSection == null) {
            return;
        }

        for (String key : auctionsSection.getKeys(false)) {
            try {
                UUID auctionId = UUID.fromString(key);
                String serializedItem = auctionsSection.getString(key + ".itemStack");
                
                if (serializedItem != null) {
                    ItemStack itemStack = deserializeItemStack(serializedItem);
                    UUID sellerUuid = UUID.fromString(auctionsSection.getString(key + ".sellerUuid"));
                    double startPrice = auctionsSection.getDouble(key + ".startPrice");
                    long expirationTime = auctionsSection.getLong(key + ".expirationTime");
                    UUID currentBidder = auctionsSection.contains(key + ".currentBidder") ?
                        UUID.fromString(auctionsSection.getString(key + ".currentBidder")) : null;
                    double currentBid = auctionsSection.getDouble(key + ".currentBid");

                    AuctionItem item = new AuctionItem(
                        auctionId,
                        itemStack,
                        sellerUuid,
                        startPrice,
                        expirationTime,
                        currentBidder,
                        currentBid
                    );

                    auctionCache.put(auctionId, item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save auctions to the configuration file.
     * This method should be called within a write lock.
     */
    private void saveToFile() throws IOException {
        config = new YamlConfiguration();
        ConfigurationSection auctionsSection = config.createSection("auctions");

        for (Map.Entry<UUID, AuctionItem> entry : auctionCache.entrySet()) {
            String key = entry.getKey().toString();
            AuctionItem item = entry.getValue();

            String serializedItem = serializeItemStack(item.getItemStack());
            auctionsSection.set(key + ".itemStack", serializedItem);
            auctionsSection.set(key + ".sellerUuid", item.getSellerUuid().toString());
            auctionsSection.set(key + ".startPrice", item.getStartPrice());
            auctionsSection.set(key + ".expirationTime", item.getExpirationTime());
            if (item.getCurrentBidder() != null) {
                auctionsSection.set(key + ".currentBidder", item.getCurrentBidder().toString());
            }
            auctionsSection.set(key + ".currentBid", item.getCurrentBid());
        }

        config.save(dataFile);
    }

    /**
     * Serialize an ItemStack to a base64-encoded string.
     * 
     * @param itemStack The ItemStack to serialize
     * @return Base64-encoded string representation
     */
    private String serializeItemStack(ItemStack itemStack) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(byteOut);

        dataOut.writeObject(itemStack);
        dataOut.close();

        return Base64.getEncoder().encodeToString(byteOut.toByteArray());
    }

    /**
     * Deserialize an ItemStack from a base64-encoded string.
     * 
     * @param serialized The base64-encoded string
     * @return Deserialized ItemStack
     */
    private ItemStack deserializeItemStack(String serialized) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(Base64.getDecoder().decode(serialized));
        BukkitObjectInputStream dataIn = new BukkitObjectInputStream(byteIn);

        ItemStack itemStack = (ItemStack) dataIn.readObject();
        dataIn.close();

        return itemStack;
    }

    /**
     * Shutdown the executor service and release resources.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
