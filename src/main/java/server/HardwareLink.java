package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Enlace con la Raspberry Pi Pico W (MicroPython).
 *
 * OJO con la direccion de la conexion: la Pico es el SERVIDOR TCP (escucha en su
 * puerto) y esta clase se conecta a ella como CLIENTE. Corre en la PC que tiene
 * el hardware (la del organizador).
 *
 * Mensajes que manda la Pico (una linea de texto cada uno):
 *   HOLA,PICO          al conectarse
 *   DADOS,d1,d2        cuando termina la tirada (cada dado de 1 a 6)
 *   TARJETA,UID        cuando el lector RFID lee una tarjeta (UID en hexadecimal)
 *
 * Si la conexion se cae (WiFi, la Pico se reinicia) reintenta cada 3 segundos.
 */
public class HardwareLink implements Runnable {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int RETRY_MS = 3000;

    private final String host;
    private final int port;
    private volatile boolean running = true;
    private volatile Socket socket;

    public HardwareLink(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Inicia el enlace en un hilo aparte que no impide cerrar la aplicacion. */
    public static HardwareLink start(String host, int port) {
        HardwareLink link = new HardwareLink(host, port);
        Thread thread = new Thread(link, "hardware-link");
        thread.setDaemon(true);
        thread.start();
        return link;
    }

    public void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // nada que hacer
        }
    }

    @Override
    public void run() {
        boolean avisoDeFalloDado = false;
        while (running) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket = s;
                avisoDeFalloDado = false;
                BankServer.broadcast("MENSAJE,Hardware conectado (" + host + ":" + port + ")");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null) {
                    processLine(line.trim());
                }
                BankServer.broadcast("MENSAJE,Se perdio la conexion con el hardware. Reintentando...");
            } catch (IOException e) {
                if (running && !avisoDeFalloDado) {
                    avisoDeFalloDado = true; // avisa una sola vez, no cada 3 segundos
                    BankServer.broadcast("MENSAJE,No se encuentra el hardware en " + host + ":" + port
                            + ". Reintentando...");
                }
            }
            if (!running) return;
            try {
                Thread.sleep(RETRY_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** Interpreta una linea de la Pico. Cualquier cosa mal formada se ignora. */
    void processLine(String line) {
        if (line.isEmpty()) return;
        String[] p = line.split(",", -1);
        switch (p[0]) {
            case "HOLA" -> System.out.println("[HARDWARE] " + line);
            case "DADOS" -> {
                try {
                    int d1 = Integer.parseInt(p[1].trim());
                    int d2 = Integer.parseInt(p[2].trim());
                    if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) {
                        System.out.println("[HARDWARE] dados fuera de rango: " + line);
                        return;
                    }
                    BankServer.hardwareRolled(d1, d2);
                } catch (RuntimeException e) { // NumberFormat / indice
                    System.out.println("[HARDWARE] linea mal formada: " + line);
                }
            }
            case "TARJETA" -> {
                if (p.length > 1 && !p[1].isBlank()) {
                    BankServer.hardwareCard(p[1].trim());
                }
            }
            default -> System.out.println("[HARDWARE] mensaje desconocido: " + line);
        }
    }
}
