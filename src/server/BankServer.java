package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BankServer {
    private static final int PORT = 8080;
    // Lista temporal (pueden cambiarla luego por su estructura personalizada si lo desean para los hilos)
    private static List<ClientHandler> connectedClients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Starting Bank Server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Bank is waiting for players to connect...");
            
            Bank mainBank = new Bank(); // Instancia del banco que manejará la lógica del juego
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New player connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Creamos un nuevo hilo para cada jugadora que se conecta
                ClientHandler clientThread = new ClientHandler(clientSocket, mainBank);
                connectedClients.add(clientThread);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    // Método para enviar actualizaciones a todos los clientes (Broadcast)
    public static void broadcastMessage(String message) {
        for (ClientHandler client : connectedClients) {
            client.sendMessage(message);
        }
    }
}