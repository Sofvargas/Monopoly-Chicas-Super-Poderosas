package monopoly_proyect;

import client.GameClient;
import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.BankServer;

public class App extends Application {

    private GameClient client;

    @Override
    public void start(Stage stage) {
        PantallaInicio inicio = new PantallaInicio(datos -> conectar(stage, datos));

        Scene scene = new Scene(inicio.getRoot(), 560, 720);
        scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Monopoly - Chicas Superpoderosas");
        stage.show();
    }

    /**
     * Se ejecuta al presionar "Conectar". Devuelve null si todo salio bien,
     * o el texto del problema para mostrarlo en la pantalla de inicio.
     */
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

        // Los mensajes llegan en un hilo de red: Platform.runLater los pasa al hilo de JavaFX
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

        stage.getScene().setRoot(interfaz.getRoot());
        stage.setWidth(1000);
        stage.setHeight(900);
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        BankServer.stopHardware();
        if (client != null) {
            client.close();
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Player;
import server.Bank;

public class App extends Application {

    // Instancia principal del Banco (nuestra lógica central)
    private Bank bank = new Bank();
    
    // Elementos visuales de la interfaz
    private Label lblTurnoInfo = new Label("Turno actual: Esperando jugadores...");
    private Label lblBalance = new Label("Saldo: $0.00");
    private Label lblPosicion = new Label("Posición: GO (0)");
    private TextArea txtConsolaLogs = new TextArea();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Monopoly - Chicas Super Poderosas (Banco Central)");

        // --- 1. PANEL SUPERIOR (Información del Jugador en Turno) ---
        VBox panelInfo = new VBox(8, lblTurnoInfo, lblBalance, lblPosicion);
        panelInfo.setPadding(new Insets(15));
        panelInfo.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc;");
        lblTurnoInfo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // --- 2. PANEL CENTRAL (Controles de Juego y Acciones) ---
        TextField txtPlayerId = new TextField();
        txtPlayerId.setPromptText("ID o Nombre del Jugador (ej. Player_1)");

        Button btnRegistrar = new Button("Registrar Jugador");
        Button btnDados = new Button("Tirar Dados");
        Button btnComprar = new Button("Comprar Propiedad Actual");
        Button btnPagarAlquiler = new Button("Pagar Alquiler");
        Button btnCarta = new Button("Sacar Carta de Evento");
        Button btnExportar = new Button("Exportar Historial TXT");

        // Estilo rápido para botones
        Button[] botonesAccion = {btnRegistrar, btnDados, btnComprar, btnPagarAlquiler, btnCarta, btnExportar};
        for (Button b : botonesAccion) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        VBox panelBotones = new VBox(10, txtPlayerId, btnRegistrar, btnDados, btnComprar, btnPagarAlquiler, btnCarta, btnExportar);
        panelBotones.setPadding(new Insets(15));

        // --- 3. PANEL INFERIOR / DERECHO (Consola de Eventos y Hardware) ---
        txtConsolaLogs.setEditable(false);
        txtConsolaLogs.setPrefHeight(150);
        txtConsolaLogs.setText("[SISTEMA] Interfaz gráfica iniciada correctamente.\n");

        VBox panelConsola = new VBox(5, new Label("Consola de Eventos y Hardware (RFID / Pico):"), txtConsolaLogs);
        panelConsola.setPadding(new Insets(10));

        // --- ACCIONES DE LOS BOTONES (Enlace con la Lógica del Bank) ---
        
        btnRegistrar.setOnAction(e -> {
            String id = txtPlayerId.getText().trim();
            if (!id.isEmpty()) {
                Player nuevo = new Player(id, id, 1500.0);
                bank.addPlayer(nuevo);
                logConsola("Jugador registrado con éxito: " + id);
                actualizarVistaEstado();
            } else {
                logConsola("ERROR: Debe ingresar un ID de jugador válido.");
            }
        });

        btnDados.setOnAction(e -> {
            String id = txtPlayerId.getText().trim();
            String resultado = bank.rollDiceAndMove(id);
            logConsola("Resultado Dados: " + resultado);
            actualizarVistaEstado();
        });

        btnComprar.setOnAction(e -> {
            String id = txtPlayerId.getText().trim();
            String resultado = bank.buyProperty(id);
            logConsola("Compra: " + resultado);
            actualizarVistaEstado();
        });

        btnPagarAlquiler.setOnAction(e -> {
            String id = txtPlayerId.getText().trim();
            String resultado = bank.payRent(id);
            logConsola("Alquiler: " + resultado);
            actualizarVistaEstado();
        });

        btnCarta.setOnAction(e -> {
            String id = txtPlayerId.getText().trim();
            String resultado = bank.drawEventCard(id);
            logConsola("Carta de Evento: " + resultado);
            actualizarVistaEstado();
        });

        btnExportar.setOnAction(e -> {
            String resultado = bank.exportTransactions();
            logConsola("Exportación: " + resultado);
        });

        // --- DISEÑO GENERAL (BorderPane) ---
        BorderPane root = new BorderPane();
        root.setTop(panelInfo);
        root.setCenter(panelBotones);
        root.setBottom(panelConsola);

        Scene scene = new Scene(root, 500, 650);
        stage.setScene(scene);
        stage.show();
    }

    private void logConsola(String mensaje) {
        txtConsolaLogs.appendText(mensaje + "\n");
    }

    private void actualizarVistaEstado() {
        Player actual = bank.getCurrentTurnPlayer();
        if (actual != null) {
            lblTurnoInfo.setText("Turno actual: " + actual.getName() + " (" + actual.getId() + ")");
            lblBalance.setText(String.format("Saldo: $%.2f", actual.getBalance()));
            lblPosicion.setText("Posición en casilla índice: " + actual.getCurrentPositionIndex());
        } else {
            lblTurnoInfo.setText("Turno actual: No hay jugadores en la cola.");
            lblBalance.setText("Saldo: $0.00");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}