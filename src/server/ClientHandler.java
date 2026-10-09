package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
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
                processCommand(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Player disconnected.");
        } finally {
            closeConnections();
        }
    }

    private void processCommand(String command) {
        // Aquí definen el protocolo solicitado en el documento
        String[] parts = command.split(",");
        String action = parts[0];

        switch (action) {
            case "CONECTAR":
                sendMessage("SUCCESS,CONNECTED_TO_BANK");
                break;
            case "TIRAR_DADOS":
                // Aquí llamarán a la lógica del dado y moverán al jugador
                BankServer.broadcastMessage("UPDATE_BOARD,Player_Rolled");
                break;
            case "COMPRAR_PROPIEDAD":
                // Validar fondos y estado del banco
                sendMessage("SUCCESS,PROPERTY_PURCHASED");
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
