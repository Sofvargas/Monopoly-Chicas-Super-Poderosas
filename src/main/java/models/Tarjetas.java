package models;

import structures.SinglyLinkedList;

/**
 * Las tarjetas RFID del juego. Lista fija: para cambiar o agregar una tarjeta
 * se edita aqui (el UID sale en el registro al acercarla al lector).
 */
public class Tarjetas {

    public static final String[] UIDS = { "F644FE9D", "61809517", "56C12B9E", "06D2299E" };
    public static final String[] ALIAS = { "Bombón", "Burbuja", "Bellota", "Mojo Jojo" };
    // Imagen del personaje (en resources/Images): es la ficha del jugador en el tablero
    public static final String[] IMAGENES = { "bombon_roja.png", "burbuja.png", "bellota.png", "mojo.png" };

    /** Lista nueva con todas las tarjetas, lista para repartir. */
    public static SinglyLinkedList<Tarjeta> crear() {
        SinglyLinkedList<Tarjeta> tarjetas = new SinglyLinkedList<>();
        for (int i = 0; i < UIDS.length; i++) {
            tarjetas.add(new Tarjeta(UIDS[i], ALIAS[i]));
        }
        return tarjetas;
    }

    /** Alias de la tarjeta con ese UID, o null si no es una tarjeta del juego. */
    public static String aliasDe(String uid) {
        for (int i = 0; i < UIDS.length; i++) {
            if (UIDS[i].equalsIgnoreCase(uid)) return ALIAS[i];
        }
        return null;
    }

    /** Archivo de imagen del personaje con ese alias, o null si no existe. */
    public static String imagenDe(String alias) {
        for (int i = 0; i < ALIAS.length; i++) {
            if (ALIAS[i].equals(alias)) return IMAGENES[i];
        }
        return null;
    }
}
