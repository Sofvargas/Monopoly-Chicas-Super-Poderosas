package models;

import java.time.LocalDateTime;

public class Transaction {
    private String id;
    private LocalDateTime dateTime;
    private int turnNumber;
    private String type; // e.g., "PURCHASE", "RENT", "BANK_PAYMENT"
    private String sourcePlayerId;
    private String destinationPlayerId; // It could be "BANK"
    private double amount;
    private String description;

    public Transaction(String id, int turnNumber, String type, String sourcePlayerId, 
                       String destinationPlayerId, double amount, String description) {
        this.id = id;
        this.dateTime = LocalDateTime.now();
        this.turnNumber = turnNumber;
        this.type = type;
        this.sourcePlayerId = sourcePlayerId;
        this.destinationPlayerId = destinationPlayerId;
        this.amount = amount;
        this.description = description;
    }

    // Getters
    public String getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public int getTurnNumber() { return turnNumber; }
    public String getType() { return type; }
    public String getSourcePlayerId() { return sourcePlayerId; }
    public String getDestinationPlayerId() { return destinationPlayerId; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("Tx[%s] Turn:%d | %s | %s -> %s : $%.2f | %s",
                id, turnNumber, type, sourcePlayerId, destinationPlayerId, amount, description);
    }
}

