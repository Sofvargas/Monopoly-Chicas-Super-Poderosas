package monopoly_proyect;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        PantallaInicio inicio = new PantallaInicio(datos -> mostrarJuego(stage, datos));

        Scene scene = new Scene(inicio.getRoot(), 560, 620);
        scene.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Monopoly - Chicas Superpoderosas");
        stage.show();
    }

    private void mostrarJuego(Stage stage, PantallaInicio.DatosConexion datos) {
        System.out.println("Jugador: " + datos.nombre() + " -> "
                + datos.ip() + ":" + datos.puerto()
                + (datos.organizador() ? " (organizador)" : ""));

        Interfaz interfaz = new Interfaz();
        stage.getScene().setRoot(interfaz.getRoot());
        stage.setWidth(1000);
        stage.setHeight(800);
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}