package monopoly_proyect;

import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class PantallaInicio {

    // Datos que se entregan cuando el jugador presiona "Conectar".
    // nombre2 puede venir vacio: es el segundo jugador que comparte esta computadora.
    public record DatosConexion(String nombre, String nombre2, String ip, int puerto, boolean organizador,
                                String ipPico, int puertoPico) {}

    private final StackPane root = new StackPane();
    private final TextField campoNombre = new TextField();
    private final TextField campoNombre2 = new TextField();
    private final TextField campoIp = new TextField("localhost");
    private final TextField campoPuerto = new TextField("8080");
    private final TextField campoIpPico = new TextField();
    private final TextField campoPuertoPico = new TextField("8080");
    private final CheckBox chkOrganizador = new CheckBox("Soy el organizador (crear la partida)");
    private final Label mensajeError = new Label();

    /**
     * 'alConectar' recibe los datos ya validados y devuelve null si todo salio bien,
     * o un texto con el problema (por ejemplo "no se pudo conectar") para mostrarlo aqui.
     */
    public PantallaInicio(Function<DatosConexion, String> alConectar) {
        // El titulo se parte en varias lineas si la ventana es angosta y su tamano
        // sale del CSS (.titulo { -fx-font-size: 2em; }), que App escala con la ventana.
        Label titulo = new Label("CHICAS SUPERPODEROSAS");
        titulo.getStyleClass().add("titulo");
        titulo.setWrapText(true);
        titulo.setTextAlignment(TextAlignment.CENTER);
        titulo.setAlignment(Pos.CENTER);
        titulo.setMaxWidth(Double.MAX_VALUE);

        Label subtitulo = new Label("Monopoly");
        subtitulo.setAlignment(Pos.CENTER);
        subtitulo.setMaxWidth(Double.MAX_VALUE);

        campoNombre.setPromptText("Tu nombre");
        campoNombre2.setPromptText("Opcional");

        // Si eres el organizador, el servidor corre en esta misma computadora:
        // la IP no se escribe.
        campoIp.disableProperty().bind(chkOrganizador.selectedProperty());

        // Solo el organizador (la PC con el hardware) usa estos campos
        campoIpPico.setPromptText("IP de la Pico (vacío = sin hardware)");
        campoIpPico.disableProperty().bind(chkOrganizador.selectedProperty().not());
        campoPuertoPico.disableProperty().bind(chkOrganizador.selectedProperty().not());

        Button btnConectar = new Button("Conectar");
        btnConectar.getStyleClass().add("boton-principal");
        btnConectar.setMaxWidth(Double.MAX_VALUE);
        btnConectar.setOnAction(e -> intentarConectar(alConectar));

        mensajeError.getStyleClass().add("mensaje-error");
        mensajeError.setWrapText(true);

        Label etiquetaPico = new Label("Hardware: IP de la Pico (solo organizador)");
        etiquetaPico.setWrapText(true);
        Label etiquetaSegundo = new Label("Segundo jugador en esta computadora");
        etiquetaSegundo.setWrapText(true);

        VBox tarjeta = new VBox(10,
                titulo, subtitulo,
                new Label("Nombre del jugador"), campoNombre,
                etiquetaSegundo, campoNombre2,
                new Label("IP del servidor"), campoIp,
                new Label("Puerto"), campoPuerto,
                chkOrganizador,
                etiquetaPico, campoIpPico,
                new Label("Hardware: puerto de la Pico"), campoPuertoPico,
                btnConectar,
                mensajeError);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        // Ancho: 90% de la ventana, pero nunca mas de 420 px
        tarjeta.maxWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.9), 420));

        // Si la ventana es baja y el formulario no cabe, aparece una barra de desplazamiento
        // en vez de cortar los campos. Si cabe, la tarjeta queda centrada.
        StackPane contenedor = new StackPane(tarjeta);
        contenedor.setPadding(new javafx.geometry.Insets(15));

        ScrollPane scroll = new ScrollPane(contenedor);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-inicio");

        root.getStyleClass().add("fondo");
        root.getChildren().add(scroll);
    }

    public Parent getRoot() {
        return root;
    }

    private void intentarConectar(Function<DatosConexion, String> alConectar) {
        String nombre = campoNombre.getText().trim();
        String nombre2 = campoNombre2.getText().trim();
        boolean organizador = chkOrganizador.isSelected();
        String ip = organizador ? "localhost" : campoIp.getText().trim();

        if (nombre.isEmpty() || ip.isEmpty()) {
            mensajeError.setText("Escribe tu nombre y la IP del servidor.");
            return;
        }
        if (!nombreValido(nombre) || (!nombre2.isEmpty() && !nombreValido(nombre2))) {
            mensajeError.setText("Los nombres no pueden llevar comas ni punto y coma.");
            return;
        }
        if (nombre.equalsIgnoreCase(nombre2)) {
            mensajeError.setText("Los dos jugadores de esta computadora deben tener nombres distintos.");
            return;
        }

        int puerto;
        try {
            puerto = Integer.parseInt(campoPuerto.getText().trim());
        } catch (NumberFormatException ex) {
            mensajeError.setText("El puerto debe ser un número.");
            return;
        }
        if (puerto < 1 || puerto > 65535) {
            mensajeError.setText("El puerto debe estar entre 1 y 65535.");
            return;
        }

        String ipPico = organizador ? campoIpPico.getText().trim() : "";
        int puertoPico = 8080;
        if (!ipPico.isEmpty()) {
            try {
                puertoPico = Integer.parseInt(campoPuertoPico.getText().trim());
            } catch (NumberFormatException ex) {
                mensajeError.setText("El puerto de la Pico debe ser un número.");
                return;
            }
            if (puertoPico < 1 || puertoPico > 65535) {
                mensajeError.setText("El puerto de la Pico debe estar entre 1 y 65535.");
                return;
            }
        }

        mensajeError.setText("");
        String error = alConectar.apply(
                new DatosConexion(nombre, nombre2, ip, puerto, organizador, ipPico, puertoPico));
        if (error != null) {
            mensajeError.setText(error);
        }
    }

    // La coma y el punto y coma son separadores del protocolo de red.
    private boolean nombreValido(String nombre) {
        return !nombre.contains(",") && !nombre.contains(";");
    }
}
