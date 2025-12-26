package ir.sunsetrq7.ubeconomy.auction;

import java.util.UUID;

public class AuctionItem {
    private final int id;
    private final UUID sellerUUID;
    private final String itemData;
    private final long price;
    private final long endTime;
    
    public AuctionItem(int id, UUID sellerUUID, String itemData, long price, long endTime) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.itemData = itemData;
        this.price = price;
        this.endTime = endTime;
    }
    
    public int getId() {
        return id;
    }
    
    public UUID getSellerUUID() {
        return sellerUUID;
    }
    
    public String getItemData() {
        return itemData;
    }
    
    public long getPrice() {
        return price;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }
}