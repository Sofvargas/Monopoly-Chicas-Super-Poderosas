package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
                if (parts.length >= 3) {
                    String playerId = parts[1];
                    String propertyId = parts[2];
                    String result = bank.processPurchase(playerId, propertyId);
                    sendMessage(result);
                } else {
                    sendMessage("ERROR,INVALID_COMMAND_FORMAT");
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
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

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
