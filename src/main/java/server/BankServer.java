package server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * El "banco": unico dueno del estado oficial de la partida.
 *
 * Corre en la computadora del organizador (la que tiene el hardware).
 * Cada COMPUTADORA que se conecta recibe un ClientHandler; una misma
 * computadora puede tener varios jugadores (por ejemplo, 2 en una ventana).
 *
 * Se puede iniciar de dos formas:
 *  1) Solo el servidor:  java -cp target/classes server.BankServer 8080
 *  2) Dentro de la aplicacion JavaFX: BankServer.startInBackground(8080)
 *     (lo que hace la casilla "Soy el organizador").
 */
public class BankServer {

    public static final int DEFAULT_PORT = 8080;

    // Lista segura para varios hilos: se agrega desde el hilo que acepta
    // conexiones y se recorre desde los hilos de cada cliente.
    private static final CopyOnWriteArrayList<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private static final GameSession session = new GameSession();

    // false = los dados los genera el servidor al recibir TIRAR_DADOS (pruebas).
    // true  = los dados llegan del hardware mediante hardwareRolled(d1, d2).
    private static volatile boolean useHardwareDice = false;

    // ---------------------------------------------------------------- inicio

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ServerSocket serverSocket = open(port);
        acceptLoop(serverSocket); // bloquea este hilo
    }

    /** Inicia el servidor en un hilo aparte (para usarlo desde la ventana JavaFX). */
    public static void startInBackground(int port) throws IOException {
        ServerSocket serverSocket = open(port); // si el puerto esta ocupado, lanza IOException aqui
        Thread thread = new Thread(() -> acceptLoop(serverSocket), "bank-accept");
        thread.setDaemon(true); // no impide cerrar la aplicacion
        thread.start();
    }

    private static ServerSocket open(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port); // escucha en todas las interfaces
        System.out.println("Bank Server escuchando en el puerto " + port);
        System.out.println("IP(s) de esta computadora: " + localAddresses());
        return serverSocket;
    }

    private static void acceptLoop(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nueva computadora conectada: " + socket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(socket);
                connectedClients.add(handler);

                Thread thread = new Thread(handler, "handler-" + socket.getInetAddress().getHostAddress());
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Servidor detenido: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- envio

    /** Envia un mensaje a TODAS las computadoras conectadas. */
    public static void broadcast(String message) {
        for (ClientHandler client : connectedClients) {
            client.send(message);
        }
    }

    static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
    }

    static GameSession session() {
        return session;
    }

    // ---------------------------------------------------------------- dados

    public static void setUseHardwareDice(boolean value) {
        useHardwareDice = value;
    }

    public static boolean usesHardwareDice() {
        return useHardwareDice;
    }

    /**
     * PUNTO DE ENTRADA DEL HARDWARE.
     * Cuando el boton fisico se oprime y la Pico/Arduino manda los dos valores,
     * el lector del puerto serial llama a este metodo. Se asume que lanza el
     * jugador en turno (el servidor valida que no haya lanzado ya).
     */
    public static void hardwareRolled(int d1, int d2) {
        String result = session.tryRoll(null);
        if (!result.equals(GameSession.OK)) {
            System.out.println("Lanzamiento del hardware ignorado: " + result);
            broadcast("MENSAJE,Tirada del hardware ignorada (" + result + ")");
            return;
        }
        announceRoll(session.currentPlayer(), d1, d2);
    }

    /**
     * Una tarjeta RFID fue leida por el hardware. Por ahora solo se avisa a todos.
     * PENDIENTE: definir que significa (identificar al jugador, pagar, etc.).
     */
    public static void hardwareCard(String uid) {
        broadcast("TARJETA," + uid);
    }

    private static volatile HardwareLink hardwareLink;

    /**
     * Se conecta a la Pico (que es servidor TCP) y activa el modo hardware:
     * desde ahora TIRAR_DADOS se rechaza y los dados solo llegan de la Pico.
     */
    public static synchronized void startHardware(String host, int port) {
        if (hardwareLink != null) hardwareLink.stop();
        hardwareLink = HardwareLink.start(host, port);
        setUseHardwareDice(true);
    }

    public static synchronized void stopHardware() {
        if (hardwareLink != null) {
            hardwareLink.stop();
            hardwareLink = null;
        }
    }

    /** Lanzamiento simulado: lo usa ClientHandler cuando no hay hardware. */
    static String rollSimulated(String player) {
        String result = session.tryRoll(player);
        if (result.equals(GameSession.OK)) {
            int d1 = ThreadLocalRandom.current().nextInt(1, 7);
            int d2 = ThreadLocalRandom.current().nextInt(1, 7);
            announceRoll(player, d1, d2);
        }
        return result;
    }

    private static void announceRoll(String player, int d1, int d2) {
        broadcast("DADOS," + player + "," + d1 + "," + d2);
        // Mueve la ficha por el tablero circular, cobra el salario y avisa a todos
        for (String line : session.applyRoll(d1, d2)) {
            broadcast(line);
        }
        // PENDIENTE (Juego/Banco): comprar propiedad, cobrar alquiler, cartas de evento.
    }

    // ---------------------------------------------------------------- ayuda

    /** IPs de esta computadora en la red (para decirle a las demas a donde conectarse). */
    public static String localAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addresses = nic.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            return "(no se pudo leer)";
        }
        return sb.length() == 0 ? "(sin red)" : sb.toString();
    }
}
