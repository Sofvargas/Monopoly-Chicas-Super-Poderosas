package models;

import java.util.concurrent.ThreadLocalRandom;
import structures.ReusableQueue;

/**
 * El mazo de cartas sorpresa. Tipos de efecto:
 *   RECEIVE_MONEY  el banco le paga 'value' al jugador
 *   PAY_MONEY      el jugador le paga 'value' al banco
 *   MOVE_FORWARD   la ficha avanza 'value' casillas
 * OJO: la descripcion no puede llevar comas (la coma separa los campos del protocolo).
 */
public class Cartas {

    private static EventCard[] todas() {
        return new EventCard[] {
            new EventCard("C1", "Error del banco a tu favor. Cobra $200", "RECEIVE_MONEY", 200),
            new EventCard("C2", "Honorarios del doctor. Paga $50", "PAY_MONEY", 50),
            new EventCard("C3", "Avanza 3 casillas", "MOVE_FORWARD", 3),
            new EventCard("C4", "Salvaste Saltadilla. El alcalde te premia con $100", "RECEIVE_MONEY", 100),
            new EventCard("C5", "Mojo Jojo destruyó tu ventana. Paga $75", "PAY_MONEY", 75),
            new EventCard("C6", "Vuelas a toda velocidad. Avanza 5 casillas", "MOVE_FORWARD", 5)
        };
    }

    /** Mazo nuevo y barajado. Cada carta que se saca vuelve al final de la cola. */
    public static ReusableQueue<EventCard> crear() {
        EventCard[] cartas = todas();
        // Baraja: intercambia cada posicion con otra al azar
        for (int i = cartas.length - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            EventCard temp = cartas[i];
            cartas[i] = cartas[j];
            cartas[j] = temp;
        }
        ReusableQueue<EventCard> mazo = new ReusableQueue<>();
        for (EventCard carta : cartas) {
            mazo.enqueue(carta);
        }
        return mazo;
    }
}
