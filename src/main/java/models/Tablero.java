package models;

import structures.CircularDoublyLinkedList;

/**
 * El tablero: 24 casillas en una lista doblemente enlazada circular.
 * El orden de NOMBRES es el mismo que dibuja la Interfaz (casilla 0 = Inicio).
 */
public class Tablero {

    public static final String[] NOMBRES = {
        "Inicio", "Laboratorio", "Carta sorpresa", "Parque", "Escuela", "Biblioteca",
        "Cárcel", "Heladería", "Museo", "Carta sorpresa", "Cine", "Estación",
        "Estacionamiento", "Pastelería", "Zoológico", "Carta sorpresa", "Plaza", "Teatro",
        "Ir a la cárcel", "Banco", "Playa", "Carta sorpresa", "Aeropuerto", "Castillo"
    };

    public static final double SALARIO_POR_VUELTA = 200;

    public static CircularDoublyLinkedList<Square> crear() {
        CircularDoublyLinkedList<Square> tablero = new CircularDoublyLinkedList<>();
        for (int i = 0; i < NOMBRES.length; i++) {
            String nombre = NOMBRES[i];
            String id = "C" + i;
            switch (nombre) {
                case "Inicio" -> tablero.insert(new SpecialSquare(id, nombre, "SALIDA"));
                case "Cárcel" -> tablero.insert(new SpecialSquare(id, nombre, "CARCEL"));
                case "Ir a la cárcel" -> tablero.insert(new SpecialSquare(id, nombre, "IR_A_CARCEL"));
                case "Estacionamiento" -> tablero.insert(new SpecialSquare(id, nombre, "DESCANSO"));
                case "Banco" -> tablero.insert(new SpecialSquare(id, nombre, "BANCO"));
                case "Carta sorpresa" -> tablero.insert(new EventSquare(id, nombre));
                default -> {
                    // Precios provisionales: suben con la posicion. Ajustar al reglamento del grupo.
                    double precio = 60 + i * 10;
                    tablero.insert(new Property(id, nombre, precio, precio / 10));
                }
            }
        }
        return tablero;
    }
}
