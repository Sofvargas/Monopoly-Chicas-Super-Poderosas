package server;

import models.Player;
import models.Square;
import models.Tablero;
import models.Transaction;
import structures.CircularDoublyLinkedList;
import structures.CircularQueue;
import structures.DoubleNode;
import structures.DoublyLinkedList;

/**
 * Estado oficial de la partida. Usa las estructuras del grupo:
 *  - CircularQueue<Player>                 -> turnos
 *  - CircularDoublyLinkedList<Square>      -> tablero
 *  - DoublyLinkedList<Transaction>         -> historial de transacciones
 * (el arreglo 'players' solo sirve para buscar por nombre; no reemplaza a ninguna estructura).
 *
 * Todos los metodos son synchronized porque varios hilos (uno por computadora
 * conectada, mas el del hardware) lo usan al mismo tiempo.
 * Los metodos devuelven OK o un codigo de error que el servidor reenvia como "ERROR,<codigo>".
 */
public class GameSession {

    public static final String OK = "OK";
    public static final int MAX_PLAYERS = 4;
    public static final double SALDO_INICIAL = 1500;

    private final Player[] players = new Player[MAX_PLAYERS];
    private int count = 0;
    private final CircularQueue<Player> turns = new CircularQueue<>();
    private final CircularDoublyLinkedList<Square> board = Tablero.crear();
    private final DoublyLinkedList<Transaction> transactions = new DoublyLinkedList<>();
    private int turnNumber = 1;
    private int transactionCounter = 0;
    private boolean started = false;
    private boolean diceRolled = false; // true si el jugador en turno ya lanzo

    /** Registra un jugador. Solo se puede antes de que inicie la partida. */
    public synchronized String addPlayer(String name) {
        if (started) return "PARTIDA_EN_CURSO";
        if (count >= MAX_PLAYERS) return "PARTIDA_LLENA";
        if (isRegistered(name)) return "NOMBRE_DUPLICADO";
        Player p = new Player("P" + (count + 1), name, SALDO_INICIAL);
        players[count++] = p;
        turns.addPlayer(p); // el orden de registro es el orden de turnos
        return OK;
    }

    public synchronized boolean isRegistered(String name) {
        return find(name) != null;
    }

    private Player find(String name) {
        for (int i = 0; i < count; i++) {
            if (players[i].getName().equalsIgnoreCase(name)) return players[i];
        }
        return null;
    }

    /** Inicia la partida. Hacen falta al menos 2 jugadores. */
    public synchronized String start() {
        if (started) return "PARTIDA_EN_CURSO";
        if (count < 2) return "FALTAN_JUGADORES";
        started = true;
        diceRolled = false;
        return OK;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    /** Nombre del jugador en turno, o null si la partida no ha iniciado. */
    public synchronized String currentPlayer() {
        return started ? turns.getCurrentTurn().getName() : null;
    }

    /** Jugadores registrados separados por ';' (ej: "Ana;Beto;Carla"). */
    public synchronized String playersAsText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(';');
            sb.append(players[i].getName());
        }
        return sb.toString();
    }

    /**
     * Intenta registrar un lanzamiento de dados.
     * 'requester' es quien lo pide; si es null, lo envia el hardware y se
     * asume que es el jugador en turno.
     */
    public synchronized String tryRoll(String requester) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (requester != null && !turns.getCurrentTurn().getName().equals(requester)) return "NO_ES_TU_TURNO";
        if (diceRolled) return "DADOS_YA_LANZADOS";
        diceRolled = true;
        return OK;
    }

    /** Termina el turno de 'name' y pasa al siguiente jugador. */
    public synchronized String endTurn(String name) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (!turns.getCurrentTurn().getName().equals(name)) return "NO_ES_TU_TURNO";
        if (!diceRolled) return "DEBES_LANZAR_PRIMERO";
        turns.advanceTurn();
        turnNumber++;
        diceRolled = false;
        return OK;
    }

    /**
     * Mueve la ficha del jugador en turno d1+d2 casillas por el tablero circular,
     * cobra el salario si pasa por Inicio y ejecuta la accion de la casilla.
     * Devuelve las lineas de protocolo que el servidor debe enviar a todos:
     *   POSICION,jugador,indice,nombreCasilla
     *   SALDO,jugador,saldo
     * (compra de propiedades y alquileres: pendiente, necesita decision del jugador).
     */
    public synchronized String[] applyRoll(int d1, int d2) {
        Player p = turns.getCurrentTurn();
        int size = board.getSize();
        int old = p.getCurrentPositionIndex();
        int total = d1 + d2;

        // Recorre el tablero con la estructura del grupo
        DoubleNode<Square> from = board.movePositions(board.getHead(), old);
        DoubleNode<Square> dest = board.movePositions(from, total);
        int newIndex = (old + total) % size;
        p.setCurrentPositionIndex(newIndex);

        boolean pasoPorInicio = old + total >= size;
        if (pasoPorInicio) {
            p.setBalance(p.getBalance() + Tablero.SALARIO_POR_VUELTA);
            transactions.addTransaction(new Transaction("T" + (++transactionCounter), turnNumber,
                    "SALARY", "BANK", p.getId(), Tablero.SALARIO_POR_VUELTA, "Paso por Inicio"));
        }

        Square square = dest.getData();
        square.executeAction(p);

        return new String[] {
            "POSICION," + p.getName() + "," + newIndex + "," + square.getName(),
            "SALDO," + p.getName() + "," + Math.round(p.getBalance())
        };
    }

    /** Historial de transacciones (para imprimir, buscar y exportar a TXT mas adelante). */
    public synchronized DoublyLinkedList<Transaction> getTransactions() {
        return transactions;
    }
}
