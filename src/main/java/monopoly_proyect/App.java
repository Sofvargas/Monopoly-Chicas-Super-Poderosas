package monopoly_proyect;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label etiqueta = new Label("Hola, JavaFX");
        Button boton = new Button("Presióname");
        boton.setOnAction(e -> etiqueta.setText("¡Funciona!"));

        VBox raiz = new VBox(10, etiqueta, boton);
        raiz.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(raiz, 300, 200));
        stage.setTitle("Mi primera app");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
