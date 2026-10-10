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
 *     TERMINAR_TURNO,jugador    pasa el turno
 *     CONSULTAR_ESTADO          pide la lista de jugadores y el turno
 *   Servidor -> Cliente
 *     OK,CONECTADO,jugador      confirmacion (solo a quien la pidio)
 *     ERROR,codigo              rechazo (solo a quien la pidio)
 *     JUGADORES,a;b;c           (a todos) lista de jugadores
 *     INICIADA,a;b;c            (a todos) la partida comenzo
 *     TURNO,jugador             (a todos) de quien es el turno
 *     DADOS,jugador,d1,d2       (a todos) resultado de los dados
 *     MENSAJE,texto             (a todos) aviso informativo
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
import java.io.PrintWriter;
import java.net.Socket;

import models.Player;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Bank bank;

    public ClientHandler(Socket socket, Bank bank) {
        this.socket = socket;
        this.bank = bank;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up client streams: " + e.getMessage());
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

    private void handleEndTurn(String player) {
        if (!owns(player)) return;
        String result = BankServer.session().endTurn(player);
        if (!result.equals(GameSession.OK)) {
            send("ERROR," + result);
            return;
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
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                processCommand(inputLine, this.bank);
            }
        } catch (IOException e) {
            System.out.println("Player disconnected.");
        } finally {
            closeConnections();
        }
    }

    private void processCommand(String command, Bank bank) {
        // Aquí definen el protocolo solicitado en el documento
        String[] parts = command.split(",");
        String action = parts[0];

        switch (action) {
            case "CONECTAR":
                Player newplayer = new Player(parts[1], parts[1] + "_Name", 1500.0);
                bank.addPlayer(newplayer);
                sendMessage("SUCCESS,CONNECTED_TO_BANK");
                break;
            case "TIRAR_DADOS":
                if (parts.length >= 2) {
                    String playerId = parts[1];
                    String rollResult = bank.rollDiceAndMove(playerId);
                    
                    if (rollResult.startsWith("SUCCESS")) {
                        // Send the result to the player who rolled
                        sendMessage(rollResult);
                        // Notify everyone else on the network that the board changed
                        BankServer.broadcastMessage("UPDATE_BOARD," + playerId + " moved.");
                    } else {
                        // Send the error (e.g., NOT_YOUR_TURN)
                        sendMessage(rollResult);
                    }
                } else {
                    sendMessage("ERROR,MISSING_PLAYER_ID");
                }
                break;
            case "COMPRAR_PROPIEDAD":
                // Validar fondos y estado del banco
                if (parts.length >= 2) {
                    String playerId = parts[1];
                    String PurchaseResult = bank.buyProperty(playerId);
                    sendMessage(PurchaseResult);
                    if (PurchaseResult.startsWith("SUCCESS")) {
                        BankServer.broadcastMessage("UPDATE_BOARD," + playerId + "_bought_property");
                        // Notify the player of the error
                        sendMessage(PurchaseResult);
                    } else {
                        // Notify everyone else on the network that the property was purchased
                        BankServer.broadcastMessage("UPDATE_PROPERTY_PURCHASED," + playerId);
                    }
                } else {
                    sendMessage("ERROR,MISSING_PLAYER_ID");
                }
                sendMessage("SUCCESS,PROPERTY_PURCHASED");
                break;
            case "TERMINAR_TURNO":
                bank.advanceTurn();
                BankServer.broadcastMessage("UPDATE, TURN_ADVANCED");
                break;
            default:
                sendMessage("ERROR,UNKNOWN_COMMAND");
                break;
        
        case "SACAR_CARTA":
                if (parts.length >= 2) {
                    String playerId = parts[1];
                    String cardResult = bank.drawEventCard(playerId);
                    sendMessage(cardResult);
                    
                    if (cardResult.startsWith("SUCCESS")) {
                        BankServer.broadcastMessage("UPDATE_BOARD," + playerId + "_drew_a_card");
                    }
                } else {
                    sendMessage("ERROR,MISSING_PLAYER_ID");
                }
                break;
        case "PAGAR_ALQUILER":
            if (parts.length >= 2) {
                String playerId = parts[1];
                String rentResult = bank.payRent(playerId);
                sendMessage(rentResult);
                    
                if (rentResult.startsWith("SUCCESS,RENT_PAID")) {
                    BankServer.broadcastMessage("UPDATE_BOARD," + playerId + "_paid_rent");
                    }
                } else {
                    sendMessage("ERROR,MISSING_PLAYER_ID");
                }
                break;
        case "EXPORTAR_HISTORIAL":
                    String exportResult = bank.exportTransactions();
                    sendMessage(exportResult);
                    break; }   
    }

    public void sendMessage(String message) {
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
    private void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
