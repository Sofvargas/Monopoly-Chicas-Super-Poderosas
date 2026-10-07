package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameClient {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Cambiar por la IP de la Mac/PC servidor en red local
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to Bank Server!");
            out.println("CONECTAR,Player_1");

            // Hilo para escuchar respuestas del servidor simultáneamente
            Thread listenerThread = new Thread(() -> {
                String serverResponse;
                try {
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println("\n[BANK] " + serverResponse);
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server lost.");
                }
            });
            listenerThread.start();

            // Ciclo para enviar comandos desde la terminal al servidor
            while (true) {
                System.out.print("Enter command (e.g., TIRAR_DADOS): ");
                String command = scanner.nextLine();
                out.println(command);
                if (command.equalsIgnoreCase("EXIT")) break;
            }

        } catch (IOException e) {
            System.err.println("Could not connect to the Bank. Is the server running?");
        }
    }
}