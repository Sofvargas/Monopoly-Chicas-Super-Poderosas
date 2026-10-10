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
