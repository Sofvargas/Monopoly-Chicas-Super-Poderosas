// Abstract base class
package models;

public abstract class Square {
    protected String id;
    protected String name;

    public Square(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    // Polimorfismo: Cada tipo de casilla ejecutará esto de forma diferente
    public abstract void executeAction(Player player);
}

