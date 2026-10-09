package models;

public class EventCard {
    private String id;
    private String description;
    private String effectType; // e.g., "RECEIVE_MONEY", "PAY_MONEY", "MOVE_FORWARD"
    private int value; // The amount of money or spaces

    public EventCard(String id, String description, String effectType, int value) {
        this.id = id;
        this.description = description;
        this.effectType = effectType;
        this.value = value;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getEffectType() { return effectType; }
    public int getValue() { return value; }
}