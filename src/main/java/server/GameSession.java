package server;

/**
 * Estado minimo de la partida que necesita la capa de red:
 * quienes juegan, si la partida ya inicio y de quien es el turno.
 *
 * - Usa arreglos a proposito (el PDF restringe List/Queue para las estructuras evaluadas).
 * - Todos los metodos son synchronized porque varios hilos (uno por computadora
 *   conectada, mas el del hardware) lo usan al mismo tiempo.
 * - Mas adelante se puede reemplazar el control de turnos por la CircularQueue
 *   del grupo, una vez corregido su metodo addPlayer.
 *
 * Los metodos devuelven OK o un codigo de error (texto) que el servidor
 * le reenvia tal cual al cliente: "ERROR,<codigo>".
 */
public class GameSession {

    public static final String OK = "OK";
    public static final int MAX_PLAYERS = 4;

    private final String[] players = new String[MAX_PLAYERS];
    private int count = 0;
    private int current = 0;          // posicion en 'players' del jugador en turno
    private boolean started = false;
    private boolean diceRolled = false; // true si el jugador en turno ya lanzo

    /** Registra un jugador. Solo se puede antes de que inicie la partida. */
    public synchronized String addPlayer(String name) {
        if (started) return "PARTIDA_EN_CURSO";
        if (count >= MAX_PLAYERS) return "PARTIDA_LLENA";
        if (isRegistered(name)) return "NOMBRE_DUPLICADO";
        players[count++] = name;
        return OK;
    }

    public synchronized boolean isRegistered(String name) {
        for (int i = 0; i < count; i++) {
            if (players[i].equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Inicia la partida. Hacen falta al menos 2 jugadores. */
    public synchronized String start() {
        if (started) return "PARTIDA_EN_CURSO";
        if (count < 2) return "FALTAN_JUGADORES";
        started = true;
        current = 0;
        diceRolled = false;
        return OK;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    /** Nombre del jugador en turno, o null si la partida no ha iniciado. */
    public synchronized String currentPlayer() {
        return started ? players[current] : null;
    }

    /** Jugadores registrados separados por ';' (ej: "Ana;Beto;Carla"). */
    public synchronized String playersAsText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(';');
            sb.append(players[i]);
        }
        return sb.toString();
    }

    /**
     * Intenta registrar un lanzamiento de dados.
     * 'requester' es quien lo pide; si es null, lo envia el hardware y se
     * asume que es el jugador en turno. Revisa y marca en un solo paso para
     * que dos lanzamientos simultaneos no pasen los dos.
     */
    public synchronized String tryRoll(String requester) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (requester != null && !players[current].equals(requester)) return "NO_ES_TU_TURNO";
        if (diceRolled) return "DADOS_YA_LANZADOS";
        diceRolled = true;
        return OK;
    }

    /** Termina el turno de 'name' y pasa al siguiente jugador. */
    public synchronized String endTurn(String name) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (!players[current].equals(name)) return "NO_ES_TU_TURNO";
        if (!diceRolled) return "DEBES_LANZAR_PRIMERO";
        current = (current + 1) % count;
        diceRolled = false;
        return OK;
    }
}
