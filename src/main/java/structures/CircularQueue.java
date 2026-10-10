// Turns
// Implementacion de una cola circular
// utilizada para gestionar el ciclo de turnos entre los jugadores de forma equitativa
// permitiendo rotar el turno actual de manera indefinida mientras permanezcan activos en la partida
package structures;

public class CircularQueue<T> {
    private node<T> front;
    private node<T> rear;
    private node<T> currentTurn; 

    public CircularQueue() {
        front = null;
        rear = null;
        currentTurn = null;
    }
    public void addPlayer(T player) {
        node<T> newNode = new node<>(player);
        if (front == null) {
            front = newNode;
            rear = newNode;
            newNode.setNext(front);
            currentTurn = front;
        } else {
            rear.setNext(newNode);   // el ultimo apunta al nuevo
            newNode.setNext(front);  // el nuevo cierra el circulo apuntando al primero
            rear.setNext(newNode);
            rear.setNext(front);
            rear = newNode;
        }
    }
    public T getCurrentTurn() {
        return (currentTurn != null) ? currentTurn.getData() : null;
    }
    public void advanceTurn() {
        if (currentTurn != null) {
            currentTurn = currentTurn.getNext();
        }
    }
}
