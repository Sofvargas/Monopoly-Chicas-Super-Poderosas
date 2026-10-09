package models;

public class Property extends Square {
    private double purchasePrice;
    private double rentPrice;
    private Player owner;

    public Property(String id, String name, double purchasePrice, double rentPrice) {
        super(id, name);
        this.purchasePrice = purchasePrice;
        this.rentPrice = rentPrice;
        this.owner = null; // null significa que el banco es dueño / está disponible
    }

    public double getPurchasePrice() { return purchasePrice; }
    public double getRentPrice() { return rentPrice; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }

    @Override
    public void executeAction(Player player) {
        // La lógica del banco validará luego si se compra o se paga alquiler
        System.out.println(player.getName() + " landed on property: " + this.name);
    }
}

    

