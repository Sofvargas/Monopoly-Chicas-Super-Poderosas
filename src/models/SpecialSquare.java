package models;

public class SpecialSquare extends Square {
    private String specialActionType;

    public SpecialSquare(String id, String name, String specialActionType) {
        super(id, name);
        this.specialActionType = specialActionType;
    }

    @Override
    public void executeAction(Player player) {
        System.out.println("Special action triggered: " + specialActionType);
    }
}