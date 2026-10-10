package server;

import java.util.concurrent.ThreadLocalRandom;
import models.Player;
import models.Property;
import models.Square;
import models.Tablero;
import models.Tarjeta;
import models.Tarjetas;
import models.Transaction;
import structures.CircularDoublyLinkedList;
import structures.CircularQueue;
import structures.DoubleNode;
import structures.DoublyLinkedList;
import structures.SinglyLinkedList;
import structures.node;

/**
 * Estado oficial de la partida. Usa las estructuras del grupo:
 *  - CircularQueue<Player>                 -> turnos
 *  - CircularDoublyLinkedList<Square>      -> tablero
 *  - DoublyLinkedList<Transaction>         -> historial de transacciones
 *  - SinglyLinkedList<Tarjeta>             -> tarjetas RFID por repartir
 * (el arreglo 'players' solo sirve para buscar por nombre; no reemplaza a ninguna estructura).
 *
 * Todos los metodos son synchronized porque varios hilos (uno por computadora
 * conectada, mas el del hardware) lo usan al mismo tiempo.
 * Los metodos devuelven OK o un codigo de error que el servidor reenvia como "ERROR,<codigo>".
 *
 * OJO: los textos de "MENSAJE,..." no pueden llevar comas (la coma separa los campos).
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
    private boolean finished = false;   // true cuando solo queda un jugador
    private boolean diceRolled = false; // true si el jugador en turno ya lanzo

    // Cobro pendiente: se ejecuta cuando 'deudor' acerca SU tarjeta.
    // Si la propiedad no tiene dueno es una COMPRA (opcional); si lo tiene es un ALQUILER (obligatorio).
    private Player deudor = null;
    private Property propiedadPendiente = null;

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

    private Player findByCard(String uid) {
        for (int i = 0; i < count; i++) {
            Tarjeta card = players[i].getCard();
            if (card != null && card.getUid().equalsIgnoreCase(uid)) return players[i];
        }
        return null;
    }

    /** Inicia la partida. Hacen falta al menos 2 jugadores y una tarjeta para cada uno. */
    public synchronized String start() {
        if (started) return "PARTIDA_EN_CURSO";
        if (count < 2) return "FALTAN_JUGADORES";
        if (count > Tarjetas.UIDS.length) return "FALTAN_TARJETAS";
        repartirTarjetas();
        started = true;
        diceRolled = false;
        return OK;
    }

    /** Le da a cada jugador una tarjeta al azar, sin repetir. */
    private void repartirTarjetas() {
        SinglyLinkedList<Tarjeta> disponibles = Tarjetas.crear();
        int restantes = Tarjetas.UIDS.length;
        for (int i = 0; i < count; i++) {
            // Avanza una cantidad al azar de nodos y saca esa tarjeta de la lista
            int salto = ThreadLocalRandom.current().nextInt(restantes);
            node<Tarjeta> actual = disponibles.getHead();
            for (int j = 0; j < salto; j++) {
                actual = actual.getNext();
            }
            players[i].setCard(actual.getData());
            disponibles.removeProperty(actual.getData());
            restantes--;
        }
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

    /** Una linea "TARJETA_ASIGNADA,jugador,alias" por jugador (despues de iniciar). */
    public synchronized String[] cardsAsLines() {
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            lines[i] = "TARJETA_ASIGNADA," + players[i].getName() + "," + players[i].getCard().getAlias();
        }
        return lines;
    }

    /** UID de la tarjeta de 'name', o null si no tiene (partida sin iniciar o jugador desconocido). */
    public synchronized String cardOf(String name) {
        Player p = find(name);
        return (p == null || p.getCard() == null) ? null : p.getCard().getUid();
    }

    /**
     * Intenta registrar un lanzamiento de dados.
     * 'requester' es quien lo pide; si es null, lo envia el hardware y se
     * asume que es el jugador en turno.
     */
    public synchronized String tryRoll(String requester) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (finished) return "PARTIDA_TERMINADA";
        if (requester != null && !turns.getCurrentTurn().getName().equals(requester)) return "NO_ES_TU_TURNO";
        if (diceRolled) return "DADOS_YA_LANZADOS";
        diceRolled = true;
        return OK;
    }

    /** Termina el turno de 'name' y pasa al siguiente jugador. */
    public synchronized String endTurn(String name) {
        if (!started) return "PARTIDA_NO_INICIADA";
        if (finished) return "PARTIDA_TERMINADA";
        if (!turns.getCurrentTurn().getName().equals(name)) return "NO_ES_TU_TURNO";
        if (!diceRolled) return "DEBES_LANZAR_PRIMERO";
        // El alquiler es obligatorio; la compra no (terminar el turno es no comprar)
        if (deudor != null && propiedadPendiente.getOwner() != null) return "PAGO_PENDIENTE";
        avanzarTurno();
        return OK;
    }

    /** Pasa el turno al siguiente jugador que siga en el juego. */
    private void avanzarTurno() {
        do {
            turns.advanceTurn();
        } while (!turns.getCurrentTurn().isActive());
        turnNumber++;
        diceRolled = false;
        deudor = null;
        propiedadPendiente = null;
    }

    /**
     * Mueve la ficha del jugador en turno d1+d2 casillas por el tablero circular,
     * cobra el salario si pasa por Inicio y ejecuta la accion de la casilla.
     * Devuelve las lineas de protocolo que el servidor debe enviar a todos:
     *   POSICION,jugador,indice,nombreCasilla
     *   SALDO,jugador,saldo
     * y, si cayo en una propiedad, las de la compra o el alquiler (ver resolverPropiedad).
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
            registrar("SALARY", "BANK", p.getId(), Tablero.SALARIO_POR_VUELTA, "Paso por Inicio");
        }

        Square square = dest.getData();
        square.executeAction(p);

        StringBuilder out = new StringBuilder();
        linea(out, "POSICION," + p.getName() + "," + newIndex + "," + square.getName());
        linea(out, saldo(p));
        if (square instanceof Property) {
            resolverPropiedad(p, (Property) square, out);
        }
        return lineas(out);
    }

    /**
     * El jugador cayo en una propiedad:
     *  - libre: queda una compra pendiente (si le alcanza el saldo)
     *  - de otro jugador: queda un alquiler pendiente, o sale del juego si no le alcanza
     *  - propia: no pasa nada
     */
    private void resolverPropiedad(Player p, Property prop, StringBuilder out) {
        Player dueno = prop.getOwner();
        if (dueno == null) {
            if (p.getBalance() < prop.getPurchasePrice()) {
                linea(out, "MENSAJE," + prop.getName() + " está libre (" + dinero(prop.getPurchasePrice())
                        + ") pero a " + p.getName() + " no le alcanza el saldo");
                return;
            }
            deudor = p;
            propiedadPendiente = prop;
            linea(out, "MENSAJE," + prop.getName() + " está libre (" + dinero(prop.getPurchasePrice()) + "). "
                    + p.getName() + ": acerca tu tarjeta para comprarla o termina el turno");
        } else if (dueno != p) {
            if (p.getBalance() < prop.getRentPrice()) {
                eliminar(p, dueno, prop, out);
                return;
            }
            deudor = p;
            propiedadPendiente = prop;
            linea(out, "MENSAJE," + p.getName() + " debe " + dinero(prop.getRentPrice()) + " de alquiler a "
                    + dueno.getName() + " por " + prop.getName() + ". Acerca tu tarjeta para pagar");
        }
    }

    /**
     * El hardware leyo una tarjeta. Si hay un cobro pendiente y la tarjeta es la
     * del jugador que debe pagar, se ejecuta; si no, solo se informa.
     * Devuelve las lineas de protocolo que el servidor debe enviar a todos.
     */
    public synchronized String[] cardTapped(String uid) {
        StringBuilder out = new StringBuilder();
        String alias = Tarjetas.aliasDe(uid);
        if (alias == null) {
            linea(out, "MENSAJE,Tarjeta desconocida: " + uid);
            return lineas(out);
        }
        Player titular = findByCard(uid);
        if (titular == null) {
            linea(out, "MENSAJE,La tarjeta " + alias + " no está asignada a ningún jugador");
            return lineas(out);
        }
        if (deudor == null) {
            linea(out, "MENSAJE,Tarjeta " + alias + ": " + titular.getName() + " tiene "
                    + dinero(titular.getBalance()));
            return lineas(out);
        }
        if (titular != deudor) {
            linea(out, "MENSAJE,Pago rechazado: la tarjeta " + alias + " es de " + titular.getName()
                    + " y debe pagar " + deudor.getName());
            return lineas(out);
        }

        if (propiedadPendiente.getOwner() == null) {
            comprar(out);
        } else {
            pagarAlquiler(out);
        }
        deudor = null;
        propiedadPendiente = null;
        return lineas(out);
    }

    private void comprar(StringBuilder out) {
        Player p = deudor;
        Property prop = propiedadPendiente;
        double precio = prop.getPurchasePrice();
        p.setBalance(p.getBalance() - precio);
        prop.setOwner(p);
        p.addProperty(prop);
        registrar("PROPERTY_PURCHASE", p.getId(), "BANK", precio, "Compra de " + prop.getName());
        linea(out, "PROPIEDAD," + p.getName() + "," + p.getCurrentPositionIndex() + "," + prop.getName());
        linea(out, saldo(p));
    }

    private void pagarAlquiler(StringBuilder out) {
        Player p = deudor;
        Property prop = propiedadPendiente;
        Player dueno = prop.getOwner();
        double renta = prop.getRentPrice();
        p.setBalance(p.getBalance() - renta);
        dueno.setBalance(dueno.getBalance() + renta);
        registrar("RENT_PAYMENT", p.getId(), dueno.getId(), renta, "Alquiler de " + prop.getName());
        linea(out, "MENSAJE," + p.getName() + " pagó " + dinero(renta) + " de alquiler a " + dueno.getName());
        linea(out, saldo(p));
        linea(out, saldo(dueno));
    }

    /**
     * 'p' no puede pagar el alquiler: sale del juego. Lo que le queda de saldo
     * pasa al dueno de la propiedad y sus propiedades vuelven a estar libres.
     * Si solo queda un jugador, gana; si no, el turno pasa al siguiente.
     */
    private void eliminar(Player p, Player acreedor, Property prop, StringBuilder out) {
        double resto = p.getBalance();
        acreedor.setBalance(acreedor.getBalance() + resto);
        p.setBalance(0);
        registrar("BANKRUPTCY", p.getId(), acreedor.getId(), resto,
                "No pudo pagar el alquiler de " + prop.getName());

        SinglyLinkedList<Property> propiedades = p.getOwnedProperties();
        while (propiedades.getHead() != null) {
            Property liberada = propiedades.getHead().getData();
            liberada.setOwner(null);
            propiedades.removeProperty(liberada);
        }
        p.setActive(false);

        linea(out, "ELIMINADO," + p.getName());
        linea(out, saldo(p));
        linea(out, saldo(acreedor));

        Player ganador = unicoActivo();
        if (ganador != null) {
            finished = true;
            deudor = null;
            propiedadPendiente = null;
            linea(out, "GANADOR," + ganador.getName());
        } else {
            avanzarTurno();
            linea(out, "TURNO," + turns.getCurrentTurn().getName());
        }
    }

    /** El unico jugador que sigue en el juego, o null si quedan dos o mas. */
    private Player unicoActivo() {
        Player unico = null;
        for (int i = 0; i < count; i++) {
            if (players[i].isActive()) {
                if (unico != null) return null;
                unico = players[i];
            }
        }
        return unico;
    }

    private void registrar(String type, String source, String destination, double amount, String description) {
        transactions.addTransaction(new Transaction("T" + (++transactionCounter), turnNumber,
                type, source, destination, amount, description));
    }

    private static String saldo(Player p) {
        return "SALDO," + p.getName() + "," + Math.round(p.getBalance());
    }

    private static String dinero(double amount) {
        return "$" + Math.round(amount);
    }

    private static void linea(StringBuilder out, String line) {
        out.append(line).append('\n');
    }

    private static String[] lineas(StringBuilder out) {
        return out.toString().split("\n");
    }

    /** Historial de transacciones (para imprimir, buscar y exportar a TXT mas adelante). */
    public synchronized DoublyLinkedList<Transaction> getTransactions() {
        return transactions;
    }
}
