package models;
import structures.SinglyLinkedList;

public class Player {
    private String id; // This can be linked to the RFID tag later
    private String name;
    private double balance;
    private int currentPositionIndex;
    private boolean isActive;
    private SinglyLinkedList<Property> ownedProperties; // Tu estructura personalizada

    public Player(String id, String name, double startingBalance) {
        this.id = id;
        this.name = name;
        this.balance = startingBalance;
        this.currentPositionIndex = 0; // Empieza en la casilla 0 (Go)
        this.isActive = true;
        this.ownedProperties = new SinglyLinkedList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    
    public int getCurrentPositionIndex() { return currentPositionIndex; }
    public void setCurrentPositionIndex(int currentPositionIndex) { this.currentPositionIndex = currentPositionIndex; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public SinglyLinkedList<Property> getOwnedProperties() { return ownedProperties; }

    public void addProperty(Property property) {
        this.ownedProperties.add(property);
    }
    public double calculateNetWorth() {
        double netWorth = this.balance;
        
        // Recorremos tu lista simple de propiedades[cite: 1]
        structures.node<models.Property> temp = this.ownedProperties.getHead();
        while (temp != null) {
            netWorth += temp.getData().getPurchasePrice();
            temp = temp.getNext();
        }
        
        return netWorth;
    }
}


