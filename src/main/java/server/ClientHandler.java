package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Atiende UNA computadora conectada (un socket).
 * Esa computadora puede tener uno o mas jugadores, por eso cada comando
 * lleva el nombre del jugador: "ACCION,jugador". El servidor solo acepta
 * ordenes para los jugadores que se registraron por ESTE socket.
 *
 * Protocolo (una linea de texto por mensaje, campos separados por coma):
 *   Cliente -> Servidor
 *     CONECTAR,jugador          registra un jugador en esta computadora
 *     INICIAR,jugador           inicia la partida (minimo 2 jugadores)
 *     TIRAR_DADOS,jugador       lanzamiento simulado (solo si no hay hardware)
 *     PASAR_TARJETA,jugador     tarjeta simulada del jugador (solo si no hay hardware)
 *     TERMINAR_TURNO,jugador    pasa el turno
 *     CONSULTAR_ESTADO          pide la lista de jugadores y el turno
 *   Servidor -> Cliente
 *     OK,CONECTADO,jugador      confirmacion (solo a quien la pidio)
 *     ERROR,codigo              rechazo (solo a quien la pidio)
 *     JUGADORES,a;b;c           (a todos) lista de jugadores
 *     INICIADA,a;b;c            (a todos) la partida comenzo
 *     TARJETA_ASIGNADA,jugador,alias   (a todos) tarjeta que le toco a cada jugador
 *     TURNO,jugador             (a todos) de quien es el turno
 *     DADOS,jugador,d1,d2       (a todos) resultado de los dados
 *     POSICION,jugador,indice,casilla  (a todos) a donde llego la ficha
 *     SALDO,jugador,saldo       (a todos) saldo nuevo de un jugador
 *     PROPIEDAD,jugador,indice,casilla (a todos) el jugador compro esa casilla
 *     CARTA,jugador,descripcion (a todos) carta sorpresa que saco el jugador
 *     ELIMINADO,jugador         (a todos) no pudo pagar un alquiler y sale del juego
 *     GANADOR,jugador           (a todos) solo queda un jugador: fin de la partida
 *     MENSAJE,texto             (a todos) aviso informativo (el texto no lleva comas)
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    /** Jugadores que juegan desde esta computadora. */
    private final CopyOnWriteArrayList<String> localPlayers = new CopyOnWriteArrayList<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // UTF-8 explicito: Windows y Mac usan codificaciones distintas por defecto
            // y los acentos de los nombres se veran mal si no se fija la misma.
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            System.err.println("Error preparando la conexion: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        if (in == null || out == null) {
            close();
            return;
        }
        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                System.out.println("Recibido: " + line);
                processCommand(line);
            }
        } catch (IOException e) {
            // la computadora se desconecto
        } finally {
            close();
        }
    }

    private void processCommand(String line) {
        String[] parts = line.split(",", -1);
        String action = parts[0].trim().toUpperCase();
        String player = parts.length > 1 ? parts[1].trim() : "";

        switch (action) {
            case "CONECTAR" -> handleConnect(player);
            case "INICIAR" -> handleStart(player);
            case "TIRAR_DADOS" -> handleRoll(player);
            case "PASAR_TARJETA" -> handleCard(player);
            case "TERMINAR_TURNO" -> handleEndTurn(player);
            case "CONSULTAR_ESTADO" -> sendState();
            default -> send("ERROR,COMANDO_DESCONOCIDO");
        }
    }

    // ------------------------------------------------------------ comandos

    private void handleConnect(String name) {
        if (name.isEmpty() || name.contains(";")) {
            send("ERROR,NOMBRE_INVALIDO");
            return;
        }
        String result = BankServer.session().addPlayer(name);
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
            return;
        }
        localPlayers.add(name);
        send("OK,CONECTADO," + name);
        BankServer.broadcast("JUGADORES," + BankServer.session().playersAsText());
    }

    private void handleStart(String player) {
        if (!owns(player)) return;
        String result = BankServer.session().start();
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
            return;
        }
        BankServer.broadcast("INICIADA," + BankServer.session().playersAsText());
        for (String line : BankServer.session().cardsAsLines()) {
            BankServer.broadcast(line);
        }
        BankServer.broadcast("TURNO," + BankServer.session().currentPlayer());
    }

    private void handleRoll(String player) {
        if (!owns(player)) return;
        if (BankServer.usesHardwareDice()) {
            send("ERROR,USA_EL_BOTON_FISICO");
            return;
        }
        String result = BankServer.rollSimulated(player);
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
        }
    }

    private void handleCard(String player) {
        if (!owns(player)) return;
        if (BankServer.usesHardwareDice()) {
            send("ERROR,USA_LA_TARJETA_FISICA");
            return;
        }
        String result = BankServer.cardSimulated(player);
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
        }
    }

    private void handleEndTurn(String player) {
        if (!owns(player)) return;
        String result = BankServer.session().endTurn(player);
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
            return;
        }
        // Avisos de jugadores en la carcel a los que se les salto el turno
        for (String line : BankServer.session().takeNotices()) {
            BankServer.broadcast(line);
        }
        BankServer.broadcast("TURNO," + BankServer.session().currentPlayer());
    }

    private void sendState() {
        GameSession session = BankServer.session();
        send("JUGADORES," + session.playersAsText());
        if (session.isStarted()) {
            send("TURNO," + session.currentPlayer());
        }
    }

    /** Una computadora solo puede actuar por los jugadores que registro ella misma. */
    private boolean owns(String player) {
        if (!localPlayers.contains(player)) {
            send("ERROR,JUGADOR_NO_REGISTRADO");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------- utilidades

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void close() {
        BankServer.removeClient(this);
        if (!localPlayers.isEmpty()) {
            BankServer.broadcast("MENSAJE,Se desconecto: " + String.join(", ", localPlayers));
        }
        try {
            socket.close(); // tambien cierra los flujos
        } catch (IOException ignored) {
            // nada que hacer
        }
    }
}

