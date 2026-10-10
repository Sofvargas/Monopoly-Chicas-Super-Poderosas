package client;

import java.io.IOException;
import java.util.Scanner;

/**
 * Cliente de consola SOLO PARA PROBAR la conexion entre computadoras,
 * sin necesidad de abrir la ventana del juego.
 *
 *   java -cp target/classes client.ConsoleClient <ip-del-servidor> <puerto>
 *
 * Ejemplo de sesion:
 *   CONECTAR,Carla
 *   INICIAR,Carla
 *   TERMINAR_TURNO,Carla      (los dados y las tarjetas solo llegan del hardware)
 *   TERMINAR_PARTIDA,Carla
 *   EXIT
 */
public class ConsoleClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "10.75.198.18";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        GameClient client = new GameClient();
        try {
            client.connect(host, port);
        } catch (IOException e) {
            System.err.println("No se pudo conectar a " + host + ":" + port + " -> " + e.getMessage());
            System.err.println("Revisa: IP correcta, servidor encendido, misma red y firewall.");
            return;
        }

        client.setOnMessage(message -> System.out.println("[BANCO] " + message));
        client.setOnDisconnect(() -> System.out.println("Se perdio la conexion con el servidor."));
        client.startListening();

        System.out.println("Conectado a " + host + ":" + port);
        System.out.println("Comandos: CONECTAR,nombre | INICIAR,nombre | TERMINAR_TURNO,nombre | TERMINAR_PARTIDA,nombre | CONSULTAR_ESTADO | EXIT");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("EXIT")) break;
                if (!line.isEmpty()) client.send(line);
            }
        }
        client.close();
    }
}
