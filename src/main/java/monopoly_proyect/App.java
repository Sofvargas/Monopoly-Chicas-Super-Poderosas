package monopoly_proyect;

import client.GameClient;
import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import server.BankServer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class App extends Application {

    private GameClient client;

    @Override
    public void start(Stage stage) {
        PantallaInicio inicio = new PantallaInicio(datos -> conectar(stage, datos));

        Scene scene = new Scene(inicio.getRoot(), 560, 720);
        scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm());
        escalarTexto(scene, inicio.getRoot());
        escalarTexto(scene, inicio.getRoot());
        stage.setScene(scene);
        stage.setTitle("Monopoly - Chicas Superpoderosas");
        stage.show();
    }

    /**
     * Se ejecuta al presionar "Conectar". Devuelve null si todo salio bien,
     * o el texto del problema para mostrarlo en la pantalla de inicio.
     */
    private void escalarTexto(Scene scene, Parent raiz) {
        // 1/45 del ancho de la ventana, entre 12 y 20 px
        raiz.styleProperty().bind(Bindings.createStringBinding(
            () -> "-fx-font-size: " + Math.max(12, Math.min(20, scene.getWidth() / 45)) + "px;",
            scene.widthProperty()));
    }

    private String conectar(Stage stage, PantallaInicio.DatosConexion datos) {
        String host = datos.ip();

        // El organizador crea el banco (servidor) dentro de esta misma aplicacion
        // y se conecta a el como cualquier otra computadora.
        if (datos.organizador()) {
            try {
                BankServer.startInBackground(datos.puerto());
            } catch (IOException e) {
                return "No se pudo crear la partida. ¿El puerto " + datos.puerto()
                        + " ya está en uso (otra partida abierta)?";
            }
            host = "localhost";
            if (!datos.ipPico().isEmpty()) {
                BankServer.startHardware(datos.ipPico(), datos.puertoPico());
            }
        }
        
        GameClient nuevo = new GameClient();
        try {
            nuevo.connect(host, datos.puerto()); // espera hasta 5 segundos
        } catch (IOException e) {
            return "No se pudo conectar a " + host + ":" + datos.puerto()
                    + ". Revisa la IP, que el servidor esté encendido y el firewall.";
        }

        client = nuevo;
        mostrarJuego(stage, datos);
        return null;
    }

    private void mostrarJuego(Stage stage, PantallaInicio.DatosConexion datos) {
        Interfaz interfaz = new Interfaz();

        // Jugadores que juegan desde ESTA computadora (1 o 2)
        String[] locales = datos.nombre2().isEmpty()
                ? new String[]{datos.nombre()}
                : new String[]{datos.nombre(), datos.nombre2()};
        interfaz.conectarRed(client::send, locales);
        interfaz.usarHardware(datos.organizador() && !datos.ipPico().isEmpty());

        //Los mensajes llegan en un hilo de red: Platform.runLater los pasa al hilo de JavaFX
        client.setOnMessage(mensaje -> Platform.runLater(() -> interfaz.procesarMensaje(mensaje)));
        client.setOnDisconnect(() -> Platform.runLater(
                () -> interfaz.mostrarMensaje("Se perdió la conexión con el servidor.")));
        client.startListening();

        // Registrar a cada jugador local en el banco (una sola conexion, varios jugadores)
        for (String nombre : locales) {
            client.send("CONECTAR," + nombre);
        }

        if (datos.organizador()) {
            interfaz.mostrarMensaje("Eres el organizador. Los demás deben conectarse a: "
                    + BankServer.localAddresses() + " (puerto " + datos.puerto() + ")");
        }
        escalarTexto(stage.getScene(), interfaz.getRoot());
        stage.getScene().setRoot(interfaz.getRoot());
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1000, pantalla.getWidth() * 0.9));
        stage.setHeight(Math.min(760, pantalla.getHeight() * 0.9));
        stage.centerOnScreen();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        BankServer.stopHardware();
        if (client != null) {
            client.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}