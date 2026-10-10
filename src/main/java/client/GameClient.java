package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Conexion de UNA computadora con el banco. No sabe nada de JavaFX:
 * la ventana le pasa una funcion que se ejecuta con cada mensaje recibido.
 *
 * Uso tipico:
 *   GameClient client = new GameClient();
 *   client.connect("192.168.1.20", 8080);                  // 1) abre el socket
 *   client.setOnMessage(msg -> ...);                        // 2) que hacer con cada mensaje
 *   client.startListening();                                // 3) empieza a escuchar
 *   client.send("CONECTAR,Ana");                            // 4) ahora si, hablar
 *
 * IMPORTANTE: los mensajes llegan en un hilo de red. Desde JavaFX hay que
 * envolver cualquier cambio de pantalla en Platform.runLater(...).
 */
public class GameClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private volatile Consumer<String> onMessage = message -> { };
    private volatile Runnable onDisconnect = () -> { };
    private volatile boolean closing = false;

    public void setOnMessage(Consumer<String> handler) {
        this.onMessage = handler;
    }

    public void setOnDisconnect(Runnable handler) {
        this.onDisconnect = handler;
    }

    /** Abre la conexion. Lanza IOException si el servidor no responde en 5 segundos. */
    public void connect(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        this.socket = s;
        this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Empieza a leer lo que manda el servidor. Llamar despues de setOnMessage. */
    public void startListening() {
        Thread thread = new Thread(this::listen, "client-listener");
        thread.setDaemon(true); // no impide cerrar la aplicacion
        thread.start();
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                onMessage.accept(line);
            }
        } catch (IOException e) {
            // conexion cerrada o perdida
        } finally {
            if (!closing) {
                onDisconnect.run();
            }
        }
    }

    /** Envia un comando al servidor, por ejemplo "TERMINAR_TURNO,Ana". */
    public void send(String command) {
        if (out != null) {
            out.println(command);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        closing = true;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // nada que hacer
        }
    }
}
// import java.io.PrintWriter;
// import java.net.Socket;
// import java.util.Scanner;

// public class GameClient {
//     private static final String SERVER_ADDRESS = "127.0.0.1"; // Cambiar por la IP de la Mac/PC servidor en red local
//     private static final int PORT = 8080;

//     public static void main(String[] args) {
//         try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
//              PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//              BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//              Scanner scanner = new Scanner(System.in)) {

//             System.out.println("Connected to Bank Server!");
//             out.println("CONECTAR,Player_1"); // cambiar por el ID real del cliente

//             // Hilo para escuchar respuestas del servidor simultáneamente
//             Thread listenerThread = new Thread(() -> {
//                 String serverResponse;
//                 try {
//                     while ((serverResponse = in.readLine()) != null) {
//                         System.out.println("\n[BANK] " + serverResponse);
//                     }
//                 } catch (IOException e) {
//                     System.out.println("Connection to server lost.");
//                 }
//             });
//             listenerThread.start();

//             // Ciclo para enviar comandos desde la terminal al servidor
//             while (true) {
//                 System.out.print("Enter command (e.g., TIRAR_DADOS): ");
//                 String command = scanner.nextLine();
//                 out.println(command);
//                 if (command.equalsIgnoreCase("EXIT")) break;
//             }

//         } catch (IOException e) {
//             System.err.println("Could not connect to the Bank. Is the server running?");
//         }
//     }
// }
