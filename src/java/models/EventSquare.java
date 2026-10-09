package models;

public class EventSquare extends Square {
    public EventSquare(String id, String name) {
        super(id, name);
    }

    @Override
    public void executeAction(Player player) {
        System.out.println(player.getName() + " must draw an event card.");
    }
}