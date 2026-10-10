package models;

/**
 * Tarjeta RFID de un jugador. Solo lo identifica: el saldo vive en el servidor.
 */
public class Tarjeta {
    private String uid;   // lo que lee el lector (hexadecimal, ej: "F644FE9D")
    private String alias; // nombre pegado en la tarjeta fisica

    public Tarjeta(String uid, String alias) {
        this.uid = uid;
        this.alias = alias;
    }

    public String getUid() { return uid; }
    public String getAlias() { return alias; }
}
