package com.sunsetrq7.ubeconomy.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * PlayerAccount - Data model for player economy account
 * Represents a player's financial state in the economy system
 */
public class PlayerAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID playerId;
    private String playerName;
    private double balance;
    private double totalEarned;
    private double totalSpent;
    private int transactionCount;
    private long lastTransactionTime;
    private boolean frozen;
    private String frozenReason;

    /**
     * Default constructor for PlayerAccount
     */
    public PlayerAccount() {
        this.balance = 0;
        this.totalEarned = 0;
        this.totalSpent = 0;
        this.transactionCount = 0;
        this.lastTransactionTime = System.currentTimeMillis();
        this.frozen = false;
        this.frozenReason = null;
    }

    /**
     * Constructor with player details
     */
    public PlayerAccount(UUID playerId, String playerName, double initialBalance) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.balance = Math.max(0, initialBalance);
    }

    // Getters and Setters

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(double totalEarned) {
        this.totalEarned = totalEarned;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getLastTransactionTime() {
        return lastTransactionTime;
    }

    public void setLastTransactionTime(long lastTransactionTime) {
        this.lastTransactionTime = lastTransactionTime;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public String getFrozenReason() {
        return frozenReason;
    }

    public void setFrozenReason(String frozenReason) {
        this.frozenReason = frozenReason;
    }

    // Business logic methods

    /**
     * Add funds to the player's account
     */
    public boolean deposit(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (frozen) {
            return false;
        }

        this.balance += amount;
        this.totalEarned += amount;
        this.transactionCount++;
        this.lastTransactionTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Remove funds from the player's account
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (frozen) {
            return false;
        }
        if (this.balance < amount) {
            return false;
        }

        this.balance -= amount;
        this.totalSpent += amount;
        this.transactionCount++;
        this.lastTransactionTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Check if player can afford a transaction
     */
    public boolean canAfford(double amount) {
        return !frozen && this.balance >= amount;
    }

    /**
     * Get net balance (totalEarned - totalSpent)
     */
    public double getNetBalance() {
        return totalEarned - totalSpent;
    }

    /**
     * Reset account statistics (admin use)
     */
    public void resetStats() {
        this.totalEarned = balance;
        this.totalSpent = 0;
        this.transactionCount = 0;
    }

    /**
     * Freeze the account
     */
    public void freezeAccount(String reason) {
        this.frozen = true;
        this.frozenReason = reason;
    }

    /**
     * Unfreeze the account
     */
    public void unfreezeAccount() {
        this.frozen = false;
        this.frozenReason = null;
    }

    @Override
    public String toString() {
        return "PlayerAccount{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", balance=" + balance +
                ", totalEarned=" + totalEarned +
                ", totalSpent=" + totalSpent +
                ", transactionCount=" + transactionCount +
                ", frozen=" + frozen +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerAccount that = (PlayerAccount) o;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}
