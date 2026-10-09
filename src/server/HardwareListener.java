package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import models.Player;

public class HardwareListener implements Runnable {
    private String picoIp;
    private int picoPort;
    private Bank bank;

    public HardwareListener(String picoIp, int picoPort, Bank bank) {
        this.picoIp = picoIp;
        this.picoPort = picoPort;
        this.bank = bank;
    }

    @Override
    public void run() {
        System.out.println("[HARDWARE] Intentando conectar a la Raspberry Pico en " + picoIp + ":" + picoPort);
        
        try (Socket socket = new Socket(picoIp, picoPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[HARDWARE] ¡Conectado exitosamente a la Raspberry Pico!");

            String inputLine;
            // Escucha constante (ciclo infinito) para recibir datos del hardware en tiempo real
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[HARDWARE] Recibido: " + inputLine);
                processHardwareCommand(inputLine.trim());
            }
        } catch (Exception e) {
            System.err.println("[HARDWARE] Error de conexión con la Raspberry. ¿Está encendida y en la misma red? " + e.getMessage());
        }
    }

    private void processHardwareCommand(String command) {
        String[] parts = command.split(",");
        String action = parts[0];
        
        // Obtener al jugador que tiene el turno actual para aplicarle la acción
        Player current = bank.getCurrentTurnPlayer(); 

        if (current == null) {
            System.out.println("[HARDWARE] Error: No hay jugadores en el turno actual para aplicar la acción.");
            return;
        }

        switch (action) {
            case "HOLA":
                System.out.println("[HARDWARE] Saludo recibido del hardware.");
                break;
                
            case "DADOS":
                if (parts.length == 3) {
                    int d1 = Integer.parseInt(parts[1]);
                    int d2 = Integer.parseInt(parts[2]);
                    
                    // Llamar a una nueva función del banco que acepta dados físicos
                    String result = bank.rollPhysicalDiceAndMove(current.getId(), d1, d2);
                    BankServer.broadcastMessage("UPDATE_BOARD,[HARDWARE_ROLL] " + result);
                }
                break;
                
            case "TARJETA":
                if (parts.length == 2) {
                    String uid = parts[1];
                    // Aquí puedes usar la tarjeta para cobrar algo, o asignarla al jugador
                    System.out.println("[HARDWARE] Tarjeta RFID detectada: " + uid);
                    
                    // Ejemplo: usar la tarjeta para confirmar el cobro de alquiler o evento
                    String cardResult = "El jugador " + current.getName() + " escaneó la tarjeta " + uid;
                    BankServer.broadcastMessage("UPDATE_BOARD,[RFID] " + cardResult);
                }
                break;
        }
    }
}