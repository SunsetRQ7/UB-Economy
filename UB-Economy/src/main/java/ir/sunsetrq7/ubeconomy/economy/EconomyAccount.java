package ir.sunsetrq7.ubeconomy.economy;

import java.util.UUID;

public class EconomyAccount {
    private final UUID uuid;
    private final long balance;
    
    public EconomyAccount(UUID uuid, long balance) {
        this.uuid = uuid;
        this.balance = balance;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public long getBalance() {
        return balance;
    }
}