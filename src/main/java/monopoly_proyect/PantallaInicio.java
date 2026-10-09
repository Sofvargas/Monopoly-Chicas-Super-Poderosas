package monopoly_proyect;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

public class PantallaInicio {

    // Datos que se entregan cuando el jugador presiona "Conectar"
    public record DatosConexion(String nombre, String ip, int puerto, boolean organizador) {}

    private final StackPane root = new StackPane();
    private final TextField campoNombre = new TextField();
    private final TextField campoIp = new TextField("localhost");
    private final TextField campoPuerto = new TextField("5000");
    private final CheckBox chkOrganizador = new CheckBox("Soy el organizador (crear la partida)");
    private final Label mensajeError = new Label();

    public PantallaInicio(Consumer<DatosConexion> alConectar) {
        Label titulo = new Label("CHICAS SUPERPODEROSAS");
        titulo.getStyleClass().add("titulo");
        Label subtitulo = new Label("Monopoly");

        campoNombre.setPromptText("Tu nombre");

        Button btnConectar = new Button("Conectar");
        btnConectar.getStyleClass().add("boton-principal");
        btnConectar.setMaxWidth(Double.MAX_VALUE);
        btnConectar.setOnAction(e -> intentarConectar(alConectar));

        mensajeError.getStyleClass().add("mensaje-error");

        VBox tarjeta = new VBox(10,
                titulo, subtitulo,
                new Label("Nombre del jugador"), campoNombre,
                new Label("IP del servidor"), campoIp,
                new Label("Puerto"), campoPuerto,
                chkOrganizador,
                btnConectar,
                mensajeError);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setMaxWidth(380);
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);

        root.getStyleClass().add("fondo");
        root.getChildren().add(tarjeta);
    }

    public Parent getRoot() {
        return root;
    }

    private void intentarConectar(Consumer<DatosConexion> alConectar) {
        String nombre = campoNombre.getText().trim();
        String ip = campoIp.getText().trim();

        if (nombre.isEmpty() || ip.isEmpty()) {
            mensajeError.setText("Escribe tu nombre y la IP del servidor.");
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

        mensajeError.setText("");
        alConectar.accept(new DatosConexion(nombre, ip, puerto, chkOrganizador.isSelected()));
    }
}