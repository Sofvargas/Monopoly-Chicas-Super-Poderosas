package server;

import models.Player;
import models.Property;
import models.Transaction;
import structures.CircularDoublyLinkedList;
import structures.CircularQueue;
import structures.DoublyLinkedList;

public class Bank {
    // Estructuras de datos oficiales del juego
    private CircularDoublyLinkedList<models.Square> board;
    private CircularQueue<Player> turnsQueue;
    private DoublyLinkedList<Transaction> transactionHistory;
    private structures.ReusableQueue<models.EventCard> eventCards;
    
    private int transactionCounter = 1;

    public Bank() {
        this.board = new CircularDoublyLinkedList<>();
        this.turnsQueue = new CircularQueue<>();
        this.transactionHistory = new DoublyLinkedList<>();
        initializeBoard();
        this.eventCards = new structures.ReusableQueue<>();
        initializeEventCards();
    }

    private void initializeBoard() {
        // Aquí debes agregar las 24 casillas mínimas que pide el documento
        // Ejemplo de las primeras casillas:
        board.insert(new models.SpecialSquare("0", "GO", "COLLECT_200"));
        board.insert(new Property("1", "Mediteranean Ave", 60, 2));
        board.insert(new models.EventSquare("2", "Community Chest"));
        board.insert(new Property("3", "Baltic Ave", 60, 4));
        // ... (Deberás completar hasta tener 24)
        System.out.println("Board initialized with " + board.getSize() + " spaces.");
    }

    public void addPlayer(Player player) {
        turnsQueue.addPlayer(player);
    }
    // Event Cards
    private void initializeEventCards() {
        eventCards.enqueue(new models.EventCard("C1", "Bank error in your favor. Collect $200", "RECEIVE_MONEY", 200));
        eventCards.enqueue(new models.EventCard("C2", "Doctor's fee. Pay $50", "PAY_MONEY", 50));
        eventCards.enqueue(new models.EventCard("C3", "Advance 3 spaces", "MOVE_FORWARD", 3));
        System.out.println("Event cards initialized.");
    }
    // 4. Agrega el método para sacar la carta y aplicar el efecto
public synchronized String drawEventCard(String playerId) {
    Player current = turnsQueue.getCurrentTurn();

    if (current == null || !current.getId().equals(playerId)) {
        return "ERROR,NOT_YOUR_TURN";
    }

    // Validar si realmente está en una casilla de evento
    int positionIndex = current.getCurrentPositionIndex();
    structures.DoubleNode<models.Square> tempNode = board.getHead();
    for (int i = 0; i < positionIndex; i++) {
        tempNode = tempNode.getNext();
    }
    
    if (!(tempNode.getData() instanceof models.EventSquare)) {
        return "ERROR,NOT_AN_EVENT_SQUARE";
    }

    // Saca la carta y la pone al final de la cola automáticamente
    models.EventCard card = eventCards.drawAndReuse(); 

    // Aplicar el efecto de la carta
    switch (card.getEffectType()) {
        case "RECEIVE_MONEY":
            current.setBalance(current.getBalance() + card.getValue());
            break;
        case "PAY_MONEY":
            if (current.getBalance() < card.getValue()) {
                return "ERROR,INSUFFICIENT_FUNDS_FOR_EVENT"; // Habría que manejar la eliminación aquí después
            }
            current.setBalance(current.getBalance() - card.getValue());
            break;
        case "MOVE_FORWARD":
            current.setCurrentPositionIndex((current.getCurrentPositionIndex() + card.getValue()) % board.getSize());
            break;
    }

    // Generar la transacción del evento[cite: 4]
    if (!card.getEffectType().equals("MOVE_FORWARD")) {
        Transaction tx = new Transaction(
            "TX" + transactionCounter++, 
            1, 
            card.getEffectType().equals("RECEIVE_MONEY") ? "EVENT_GAIN" : "EVENT_LOSS", 
            card.getEffectType().equals("RECEIVE_MONEY") ? "BANK" : playerId, 
            card.getEffectType().equals("RECEIVE_MONEY") ? playerId : "BANK", 
            card.getValue(), 
            "Card: " + card.getDescription()
        );
        transactionHistory.addTransaction(tx);
    }

    return String.format("SUCCESS,CARD_DRAWN:%s,NEW_BALANCE:%.2f", card.getDescription(), current.getBalance());
}

    // Ejemplo de validación centralizada
    public synchronized String processPurchase(String playerId, String propertyId) {
        Player current = turnsQueue.getCurrentTurn();
        
        // 1. Validar si es el turno del jugador
        if (current == null || !current.getId().equals(playerId)) {
            return "ERROR,NOT_YOUR_TURN";
        }

        // (Aquí iría la lógica para buscar la propiedad en el tablero...)
        // Simularemos que encontramos la propiedad y cuesta 60:
        double price = 60.0; 

        // 2. Validar fondos
        if (current.getBalance() < price) {
            return "ERROR,INSUFFICIENT_FUNDS";
        }

        // 3. Ejecutar compra
        current.setBalance(current.getBalance() - price);
        
        // 4. Generar y guardar la transacción en tu estructura doblemente enlazada
        Transaction tx = new Transaction(
            "TX" + transactionCounter++, 
            1, // (Aquí iría el número de turno real)
            "PROPERTY_PURCHASE", 
            playerId, 
            "BANK", 
            price, 
            "Bought property " + propertyId
        );
        transactionHistory.addTransaction(tx);

        return "SUCCESS,NEW_BALANCE:" + current.getBalance();
    }
    
    public void advanceTurn() {
        turnsQueue.advanceTurn();
    }

// Add this method inside your Bank class
public synchronized String rollDiceAndMove(String playerId) {
    Player current = turnsQueue.getCurrentTurn();
    
    // 1. Validate if it's the player's turn
    if (current == null || !current.getId().equals(playerId)) {
        return "ERROR,NOT_YOUR_TURN";
    }

    // 2. Roll two dice (1 to 6 each)
    int dice1 = (int)(Math.random() * 6) + 1;
    int dice2 = (int)(Math.random() * 6) + 1;
    int totalMove = dice1 + dice2;

    // 3. Move the player across the circular doubly linked list nodes
    // Get current position index, calculate new one
    int oldIndex = current.getCurrentPositionIndex();
    int newIndex = (oldIndex + totalMove) % board.getSize();
    if (newIndex < oldIndex) {
        double goBonus = 200.0;
        current.setBalance(current.getBalance() + goBonus);
        Transaction tx = new Transaction(
            "TX" + transactionCounter++, 1, "GO_BONUS", "BANK", playerId, goBonus, "Passed GO"
        );
        transactionHistory.addTransaction(tx);
    }
    current.setCurrentPositionIndex(newIndex);

    // Traverse the nodes to find the landed square
    structures.DoubleNode<models.Square> tempNode = board.getHead();
    for (int i = 0; i < newIndex; i++) {
        tempNode = tempNode.getNext();
    }
    
    models.Square landedSquare = tempNode.getData();

    // 4. Return the result to the client
    return String.format("SUCCESS,ROLLED:%d,LANDED_ON:%s,SQUARE_ID:%s", 
                         totalMove, landedSquare.getName(), landedSquare.getId());
}
// Agrega este método dentro de tu clase Bank
public synchronized String buyProperty(String playerId) {
    Player current = turnsQueue.getCurrentTurn();

    // 1. Validar que sea el turno del jugador
    if (current == null || !current.getId().equals(playerId)) {
        return "ERROR,NOT_YOUR_TURN";
    }

    // 2. Encontrar en qué casilla está parado el jugador actualmente
    int positionIndex = current.getCurrentPositionIndex();
    structures.DoubleNode<models.Square> tempNode = board.getHead();
    for (int i = 0; i < positionIndex; i++) {
        tempNode = tempNode.getNext();
    }
    
    models.Square currentSquare = tempNode.getData();

    // 3. Validar que la casilla sea del tipo "Propiedad"
    if (!(currentSquare instanceof Property)) {
        return "ERROR,NOT_A_PROPERTY";
    }

    Property property = (Property) currentSquare;

    // 4. Validar que no tenga dueño ya[cite: 6]
    if (property.getOwner() != null) {
        return "ERROR,ALREADY_OWNED";
    }

    // 5. Validar fondos suficientes[cite: 6]
    if (current.getBalance() < property.getPurchasePrice()) {
        return "ERROR,INSUFFICIENT_FUNDS";
    }

    // 6. Ejecutar la compra
    current.setBalance(current.getBalance() - property.getPurchasePrice());
    property.setOwner(current);
    current.addProperty(property); // Guarda en la lista simple del jugador[cite: 1]

    // 7. Generar y guardar la transacción[cite: 4]
    Transaction tx = new Transaction(
        "TX" + transactionCounter++, 
        1, // Número de turno (puedes crear un contador de turnos global luego)
        "PROPERTY_PURCHASE", 
        playerId, 
        "BANK", 
        property.getPurchasePrice(), 
        "Bought " + property.getName()
    );
    transactionHistory.addTransaction(tx);

    return String.format("SUCCESS,BOUGHT:%s,NEW_BALANCE:%.2f", property.getName(), current.getBalance());
}
public synchronized String payRent(String playerId) {
    Player current = turnsQueue.getCurrentTurn();

    // 1. Validar turno
    if (current == null || !current.getId().equals(playerId)) {
        return "ERROR,NOT_YOUR_TURN";
    }

    // 2. Obtener la casilla actual
    int positionIndex = current.getCurrentPositionIndex();
    structures.DoubleNode<models.Square> tempNode = board.getHead();
    for (int i = 0; i < positionIndex; i++) {
        tempNode = tempNode.getNext();
    }

    models.Square currentSquare = tempNode.getData();

    // 3. Validaciones de la propiedad
    if (!(currentSquare instanceof Property)) {
        return "ERROR,NOT_A_PROPERTY";
    }

    Property property = (Property) currentSquare;
    Player owner = property.getOwner();

    if (owner == null) {
        return "ERROR,PROPERTY_UNOWNED"; // Está libre, debería usar COMPRAR_PROPIEDAD
    }

    if (owner.getId().equals(playerId)) {
        return "SUCCESS,OWN_PROPERTY_NO_RENT"; // Si es el mismo jugador, no se realiza pago[cite: 3]
    }

    double rent = property.getRentPrice();

    // 4. Validar fondos (La regla de eliminación si no puede pagar se maneja aquí después)
    if (current.getBalance() < rent) {
        return "ERROR,INSUFFICIENT_FUNDS_FOR_RENT"; 
    }

    // 5. Ejecutar la transferencia entre jugadores
    current.setBalance(current.getBalance() - rent);
    owner.setBalance(owner.getBalance() + rent);

    // 6. Registrar la transacción del alquiler[cite: 3]
    Transaction tx = new Transaction(
        "TX" + transactionCounter++,
        1, // Número de turno
        "RENT_PAYMENT", // Pago entre jugadores[cite: 3]
        playerId,
        owner.getId(),
        rent,
        "Paid rent for " + property.getName()
    );
    transactionHistory.addTransaction(tx);

    return String.format("SUCCESS,RENT_PAID:%.2f,TO:%s,NEW_BALANCE:%.2f", rent, owner.getName(), current.getBalance());
}
public synchronized String exportTransactions() {
    // Formato mínimo obligatorio: TXT
    String filename = "historial_transacciones.txt";
    
    try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
        writer.write("ID\tTURN\tTYPE\tSOURCE\tDESTINATION\tAMOUNT\tDESCRIPTION\n");
        writer.write("----------------------------------------------------------------------\n");
        
        // Usamos tu lista doblemente enlazada para recorrer desde la más antigua[cite: 5]
        structures.DoubleNode<models.Transaction> temp = transactionHistory.getOldest();
        
        while (temp != null) {
            models.Transaction tx = temp.getData();
            writer.write(String.format("%s\t%d\t%s\t%s\t%s\t%.2f\t%s\n",
                tx.getId(), tx.getTurnNumber(), tx.getType(), 
                tx.getSourcePlayerId(), tx.getDestinationPlayerId(), 
                tx.getAmount(), tx.getDescription()
            ));
            temp = temp.getNext();
        }
        return "SUCCESS,TRANSACTIONS_EXPORTED";
    } catch (java.io.IOException e) {
        return "ERROR,EXPORT_FAILED";
    }
}
}