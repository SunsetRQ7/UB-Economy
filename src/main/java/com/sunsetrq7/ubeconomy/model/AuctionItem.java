package com.sunsetrq7.ubeconomy.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model class representing an item in the auction system.
 * Stores all relevant information about an auctioned item including
 * seller, bidding details, and auction timing.
 */
public class AuctionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private UUID sellerId;
    private String sellerName;
    private String itemName;
    private String itemDescription;
    private double startingPrice;
    private double currentBid;
    private UUID currentBidderUUID;
    private String currentBidderName;
    private int bidCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private AuctionStatus status;
    private String itemData; // Serialized item data for storage
    private double quantity;
    private boolean cancelled;

    /**
     * Enum representing the status of an auction item.
     */
    public enum AuctionStatus {
        ACTIVE("Active"),
        SOLD("Sold"),
        EXPIRED("Expired"),
        CANCELLED("Cancelled");

        private final String displayName;

        AuctionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Default constructor for AuctionItem.
     */
    public AuctionItem() {
        this.id = UUID.randomUUID().toString();
        this.currentBid = 0;
        this.bidCount = 0;
        this.createdAt = LocalDateTime.now();
        this.status = AuctionStatus.ACTIVE;
        this.cancelled = false;
        this.quantity = 1;
    }

    /**
     * Constructor with essential auction parameters.
     */
    public AuctionItem(UUID sellerId, String sellerName, String itemName, double startingPrice, LocalDateTime expiresAt) {
        this();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public UUID getCurrentBidderUUID() {
        return currentBidderUUID;
    }

    public void setCurrentBidderUUID(UUID currentBidderUUID) {
        this.currentBidderUUID = currentBidderUUID;
    }

    public String getCurrentBidderName() {
        return currentBidderName;
    }

    public void setCurrentBidderName(String currentBidderName) {
        this.currentBidderName = currentBidderName;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    public void incrementBidCount() {
        this.bidCount++;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // Business logic methods

    /**
     * Checks if the auction is still active.
     *
     * @return true if the auction has not expired and is not cancelled
     */
    public boolean isActive() {
        return !cancelled && status == AuctionStatus.ACTIVE && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Checks if the auction has expired.
     *
     * @return true if the current time is past the expiration time
     */
    public boolean hasExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Places a bid on this auction item.
     *
     * @param bidderUUID the UUID of the bidder
     * @param bidderName the name of the bidder
     * @param bidAmount the amount being bid
     * @return true if the bid was successful, false otherwise
     */
    public boolean placeBid(UUID bidderUUID, String bidderName, double bidAmount) {
        if (!isActive()) {
            return false;
        }

        if (bidAmount <= currentBid) {
            return false;
        }

        this.currentBidderUUID = bidderUUID;
        this.currentBidderName = bidderName;
        this.currentBid = bidAmount;
        this.bidCount++;
        return true;
    }

    /**
     * Gets the time remaining for this auction in seconds.
     *
     * @return the number of seconds until auction expires, or -1 if already expired
     */
    public long getTimeRemainingSeconds() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.SECONDS.between(now, expiresAt);
    }

    @Override
    public String toString() {
        return "AuctionItem{" +
                "id='" + id + '\'' +
                ", sellerName='" + sellerName + '\'' +
                ", itemName='" + itemName + '\'' +
                ", startingPrice=" + startingPrice +
                ", currentBid=" + currentBid +
                ", currentBidderName='" + currentBidderName + '\'' +
                ", bidCount=" + bidCount +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuctionItem that = (AuctionItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
