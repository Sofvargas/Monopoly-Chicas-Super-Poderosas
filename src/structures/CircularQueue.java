// Turns
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
