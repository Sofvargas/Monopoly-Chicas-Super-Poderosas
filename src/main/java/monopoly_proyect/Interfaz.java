package monopoly_proyect;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
public class Interfaz {

    private static final int TOTAL_CASILLAS = 24;
    private static final String[] NOMBRES = {
    "Inicio", "Laboratorio", "Carta sorpresa", "Parque", "Escuela", "Biblioteca",
    "Cárcel", "Heladería", "Museo", "Carta sorpresa", "Cine", "Estación",
    "Estacionamiento", "Pastelería", "Zoológico", "Carta sorpresa", "Plaza", "Teatro",
    "Ir a la cárcel", "Banco", "Playa", "Carta sorpresa", "Aeropuerto", "Castillo"
};
    private static final int LADO = TOTAL_CASILLAS / 4; // 6 casillas por lado

    private final BorderPane root = new BorderPane();
    private final GridPane tablero = new GridPane();

    public Interfaz() {
        root.getStyleClass().add("fondo");
        tablero.getStyleClass().add("tablero");
        tablero.setAlignment(Pos.CENTER);
    
        construirTablero();
        root.setCenter(tablero);
    }

    public Parent getRoot() {
        return root;
    }

    private void construirTablero() {
        for (int i = 0; i < TOTAL_CASILLAS; i++) {
            int[] pos = posicion(i);
           tablero.add(crearCasilla(NOMBRES[i], i), pos[1], pos[0]);
        }

        tablero.add(crearCentro(), 1, 1, LADO - 1, LADO - 1);
    }
    private StackPane crearCentro() {
        Image imagen = new Image(getClass().getResourceAsStream("/Images/fondo.png"));
        ImageView vista = new ImageView(imagen);
        vista.setFitWidth(420);
        vista.setFitHeight(420);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);

        StackPane centro = new StackPane(vista);
        centro.getStyleClass().add("centro-tablero");
        return centro;
}

    private StackPane crearCasilla(String texto, int indice) {
        Label etiqueta = new Label(texto);
        etiqueta.setWrapText(true);
        etiqueta.getStyleClass().add("texto-casilla");

        StackPane celda = new StackPane(etiqueta);
        celda.setPrefSize(90, 90);
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