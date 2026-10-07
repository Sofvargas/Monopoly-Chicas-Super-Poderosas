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
    
    private int transactionCounter = 1;

    public Bank() {
        this.board = new CircularDoublyLinkedList<>();
        this.turnsQueue = new CircularQueue<>();
        this.transactionHistory = new DoublyLinkedList<>();
        initializeBoard();
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
}}