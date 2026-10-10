package monopoly_proyect;

import java.io.InputStream;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Tablero;

public class Interfaz {

    private static final int TOTAL_CASILLAS = 24;
    private static final String[] NOMBRES = Tablero.NOMBRES; // mismo orden que el tablero del servidor
    private static final int LADO = TOTAL_CASILLAS / 4;      // 6 casillas por lado (el tablero es de 7x7)

    private static final double ANCHO_PANEL = 280;           // panel derecho
    private static final double MARGEN = 40;                 // margen alrededor del tablero

    private final BorderPane root = new BorderPane();
    private final GridPane tablero = new GridPane();

    // ----- Red -----
    private final Label lblTurno = new Label("Esperando jugadores...");
    private final Button btnIniciar = new Button("Iniciar partida");
    private final Button btnTerminar = new Button("Terminar turno");
    private final Button btnTerminarPartida = new Button("Terminar partida");
    private final TextArea registro = new TextArea();

    // ----- Historial de transacciones (llega del servidor al terminar la partida) -----
    // Cada fila: {id, turno, tipo, de, para, monto, descripcion}
    private final ObservableList<String[]> historial = FXCollections.observableArrayList();
    private String quienTermino = "";
    private Stage ventanaHistorial;

    private Consumer<String> enviarComando = comando -> { };
    private String[] jugadoresLocales = new String[0];
    private String turnoActual = "";

    // Tamano (en pixeles) de cada casilla; se recalcula solo al cambiar la ventana
    private DoubleBinding tamanoCasilla;

    public Interfaz() {
        root.getStyleClass().add("fondo");
        tablero.getStyleClass().add("tablero");
        tablero.setAlignment(Pos.CENTER);

        // IMPORTANTE: esto va ANTES de construirTablero(), que ya usa tamanoCasilla
        tamanoCasilla = Bindings.createDoubleBinding(() -> {
            double ancho = root.getWidth() - ANCHO_PANEL - MARGEN;
            double alto = root.getHeight() - MARGEN;
            return Math.max(50, Math.min(ancho, alto) / (LADO + 1)); // 7 casillas por lado
        }, root.widthProperty(), root.heightProperty());

        construirTablero();
        root.setCenter(tablero);
        BorderPane.setMargin(tablero, new Insets(10));

        // ----- Panel derecho: turno, botones y registro -----
        lblTurno.getStyleClass().add("titulo");
        lblTurno.setWrapText(true);

        registro.setEditable(false);
        registro.setWrapText(true);

        btnIniciar.getStyleClass().add("boton-principal");
        btnTerminar.getStyleClass().add("boton-principal");
        btnTerminar.setDisable(true);
        btnIniciar.setOnAction(e -> {
            if (jugadoresLocales.length > 0) {
                enviarComando.accept("INICIAR," + jugadoresLocales[0]);
            }
        });
        btnTerminar.setOnAction(e -> enviarComando.accept("TERMINAR_TURNO," + turnoActual));

        // "Terminar partida": cualquiera de las computadoras puede pulsarlo (no depende
        // del turno). Termina la partida para TODOS y el servidor responde con el historial.
        // Los dados y las tarjetas no tienen boton: solo llegan del hardware.
        btnTerminarPartida.getStyleClass().add("boton-principal");
        btnTerminarPartida.setDisable(true); // se activa cuando la partida inicia
        btnTerminarPartida.setOnAction(e -> pedirTerminarPartida());

        // Que los botones ocupen todo el ancho del panel
        btnIniciar.setMaxWidth(Double.MAX_VALUE);
        btnTerminar.setMaxWidth(Double.MAX_VALUE);
        btnTerminarPartida.setMaxWidth(Double.MAX_VALUE);

        VBox panel = new VBox(10, lblTurno, btnIniciar, btnTerminar, btnTerminarPartida, registro);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(ANCHO_PANEL);
        panel.setMinWidth(ANCHO_PANEL);
        panel.setMaxWidth(ANCHO_PANEL);
        VBox.setVgrow(registro, Priority.ALWAYS); // el registro ocupa todo el alto sobrante
        root.setRight(panel);
    }

    public Parent getRoot() {
        return root;
    }

    // ================================================================ RED

    /**
     * Conecta la ventana con la red.
     * @param enviar    funcion que manda un comando al servidor (client::send)
     * @param locales   nombres de los jugadores que juegan desde esta computadora
     */
    public void conectarRed(Consumer<String> enviar, String... locales) {
        this.enviarComando = enviar;
        this.jugadoresLocales = locales;
    }

    /** Interpreta cada mensaje del servidor. Siempre se llama desde el hilo de JavaFX. */
    public void procesarMensaje(String linea) {
        String[] p = linea.split(",", -1);
        switch (p[0]) {
            case "JUGADORES" -> mostrarMensaje("Jugadores conectados: " + (p.length > 1 ? p[1].replace(";", ", ") : ""));
            case "INICIADA" -> {
                btnIniciar.setDisable(true);
                btnTerminarPartida.setDisable(false);
                mostrarMensaje("¡La partida comenzó!");
            }
            case "TURNO" -> {
                turnoActual = p.length > 1 ? p[1] : "";
                lblTurno.setText("Turno de: " + turnoActual);
                boolean leToca = esLocal(turnoActual); // true solo si le toca a alguien de ESTA computadora
                btnTerminar.setDisable(!leToca);
                mostrarMensaje("Turno de " + turnoActual);
            }
            case "TARJETA_ASIGNADA" -> mostrarMensaje(p[1] + " recibió la tarjeta " + p[2]);
            case "CARTA" -> mostrarMensaje(p[1] + " sacó una carta: " + p[2]);
            case "PROPIEDAD" -> mostrarMensaje(p[1] + " compró " + p[3]);
            case "ELIMINADO" -> mostrarMensaje(p[1] + " no pudo pagar y quedó fuera del juego. Sus propiedades quedan libres.");
            case "GANADOR" -> {
                lblTurno.setText("¡Ganó " + p[1] + "!");
                btnTerminar.setDisable(true);
                mostrarMensaje("Fin de la partida. ¡Ganó " + p[1] + "! Pulsa \"Terminar partida\" para ver el historial.");
            }
            // El historial llega en varias lineas: INICIO, una TRANSACCION por fila y FIN
            case "HISTORIAL_INICIO" -> {
                historial.clear();
                quienTermino = p.length > 1 ? p[1] : "";
            }
            case "TRANSACCION" -> agregarTransaccion(p);
            case "HISTORIAL_FIN" -> {
                btnTerminar.setDisable(true);
                if (!lblTurno.getText().startsWith("¡Ganó")) {
                    lblTurno.setText("Partida terminada");
                }
                mostrarMensaje(quienTermino + " terminó la partida.");
                mostrarHistorial();
            }
            case "DADOS" -> mostrarDados(p, linea);
            case "POSICION" -> mostrarMensaje(p[1] + " avanzó a " + p[3] + " (casilla " + p[2] + ")");
            case "SALDO" -> mostrarMensaje("Saldo de " + p[1] + ": $" + p[2]);
            case "TARJETA" -> mostrarMensaje("Tarjeta leída: " + (p.length > 1 ? p[1] : ""));
            case "ERROR" -> mostrarMensaje("Error: " + (p.length > 1 ? p[1] : linea));
            case "OK" -> { /* confirmacion que no hace falta mostrar */ }
            case "MENSAJE" -> mostrarMensaje(p.length > 1 ? p[1] : "");
            default -> mostrarMensaje(linea);
        }
    }

    private void mostrarDados(String[] p, String linea) {
        try {
            int d1 = Integer.parseInt(p[2]);
            int d2 = Integer.parseInt(p[3]);
            mostrarMensaje(p[1] + " sacó " + d1 + " y " + d2 + " (total " + (d1 + d2) + ")");
        } catch (RuntimeException e) {
            mostrarMensaje(linea); // mensaje mal formado: se muestra tal cual
        }
    }

    private boolean esLocal(String nombre) {
        for (String local : jugadoresLocales) {
            if (local.equals(nombre)) return true;
        }
        return false;
    }

    public void mostrarMensaje(String texto) {
        registro.appendText(texto + "\n");
    }

    // ========================================================== HISTORIAL

    /** Pide confirmacion (termina la partida de todos) y avisa al servidor. */
    private void pedirTerminarPartida() {
        if (jugadoresLocales.length == 0) return;
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "La partida se termina para todos los jugadores y se muestra el historial.",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Terminar partida");
        confirmacion.setHeaderText("¿Terminar la partida?");
        confirmacion.initOwner(root.getScene().getWindow());
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            enviarComando.accept("TERMINAR_PARTIDA," + jugadoresLocales[0]);
        }
    }

    /** Guarda una linea "TRANSACCION,id,turno,tipo,origen,destino,monto,descripcion" como fila. */
    private void agregarTransaccion(String[] p) {
        if (p.length < 8) return; // linea mal formada
        historial.add(new String[] { p[1], p[2], tipoLegible(p[3]), p[4], p[5], "$" + p[6], p[7] });
    }

    private String tipoLegible(String tipo) {
        return switch (tipo) {
            case "PROPERTY_PURCHASE" -> "Compra";
            case "RENT_PAYMENT" -> "Alquiler";
            case "SALARY" -> "Salario";
            case "EVENT_GAIN" -> "Carta (cobro)";
            case "EVENT_LOSS" -> "Carta (pago)";
            case "BANKRUPTCY" -> "Quiebra";
            default -> tipo;
        };
    }

    /** Ventana flotante con el historial de transacciones y el boton para salir del juego. */
    private void mostrarHistorial() {
        if (ventanaHistorial != null) {
            ventanaHistorial.close(); // si alguien volvio a pulsar, se reemplaza la anterior
        }

        Label titulo = new Label("Historial de transacciones");
        titulo.getStyleClass().add("titulo");
        Label detalle = new Label(quienTermino + " terminó la partida. Transacciones: " + historial.size());

        TableView<String[]> tabla = new TableView<>(historial);
        tabla.setPlaceholder(new Label("No hubo transacciones en esta partida."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        String[] encabezados = { "#", "Turno", "Tipo", "De", "Para", "Monto", "Descripción" };
        for (int i = 0; i < encabezados.length; i++) {
            int campo = i;
            TableColumn<String[], String> columna = new TableColumn<>(encabezados[i]);
            columna.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue()[campo]));
            columna.setSortable(false); // el orden es el de la partida
            tabla.getColumns().add(columna);
        }
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button btnSalir = new Button("Salir del juego");
        btnSalir.getStyleClass().add("boton-principal");
        btnSalir.setMaxWidth(Double.MAX_VALUE);
        btnSalir.setOnAction(e -> Platform.exit()); // cierra la aplicacion (App.stop cierra la red)

        VBox contenido = new VBox(10, titulo, detalle, tabla, btnSalir);
        contenido.setPadding(new Insets(15));
        contenido.getStyleClass().add("fondo");

        Scene escena = new Scene(contenido, 780, 480);
        escena.getStylesheets().addAll(root.getScene().getStylesheets());

        ventanaHistorial = new Stage();
        ventanaHistorial.setTitle("Historial de transacciones");
        ventanaHistorial.initOwner(root.getScene().getWindow());
        ventanaHistorial.initModality(Modality.WINDOW_MODAL);
        ventanaHistorial.setScene(escena);
        ventanaHistorial.show();
    }

    // ============================================================ TABLERO

    private void construirTablero() {
        for (int i = 0; i < TOTAL_CASILLAS; i++) {
            int[] pos = posicion(i);
            tablero.add(crearCasilla(NOMBRES[i], i), pos[1], pos[0]);
        }

        tablero.add(crearCentro(), 1, 1, LADO - 1, LADO - 1);
    }

    private StackPane crearCentro() {
        StackPane centro = new StackPane();
        centro.getStyleClass().add("centro-tablero");

        // Si la imagen no se encuentra (nombre o mayusculas distintas) no se cae el programa
        InputStream flujo = getClass().getResourceAsStream("/Images/fondo.png");
        if (flujo == null) {
            centro.getChildren().add(new Label("CHICAS SUPERPODEROSAS"));
            return centro;
        }

        ImageView vista = new ImageView(new Image(flujo));
        vista.fitWidthProperty().bind(tamanoCasilla.multiply(LADO - 1).subtract(20));
        vista.fitHeightProperty().bind(tamanoCasilla.multiply(LADO - 1).subtract(20));
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        centro.getChildren().add(vista);
        return centro;
    }

    private StackPane crearCasilla(String texto, int indice) {
        Label etiqueta = new Label(texto);
        etiqueta.setWrapText(true);
        etiqueta.getStyleClass().add("texto-casilla");

        StackPane celda = new StackPane(etiqueta);
        celda.prefWidthProperty().bind(tamanoCasilla);
        celda.prefHeightProperty().bind(tamanoCasilla);
        celda.setMinSize(0, 0); // permite que la casilla se encoja con la ventana
        celda.getStyleClass().add("casilla");
        celda.getStyleClass().add(claseDeColor(indice));
        return celda;
    }

    // Provisional: despues el color saldra del tipo de casilla real
    private String claseDeColor(int i) {
        if (i == 0) return "casilla-inicio";
        return switch (i % 3) {
            case 0 -> "grupo-rosa";
            case 1 -> "grupo-azul";
            default -> "grupo-verde";
        };
    }

    // Convierte el indice 0..23 en {fila, columna} recorriendo el borde
    private int[] posicion(int i) {
        if (i <= LADO)     return new int[]{LADO, LADO - i};            // abajo, derecha a izquierda
        if (i <= 2 * LADO) return new int[]{LADO - (i - LADO), 0};      // izquierda, de abajo hacia arriba
        if (i <= 3 * LADO) return new int[]{0, i - 2 * LADO};           // arriba, izquierda a derecha
        return new int[]{i - 3 * LADO, LADO};                           // derecha, de arriba hacia abajo
    }
}