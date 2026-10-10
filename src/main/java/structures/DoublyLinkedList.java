// Transaction History
// Implementacion de una lista doblemente enlazada generica
// facilita el recorrido bidireccional y la manipulacion eficiente de nodos para el manejo de historiales y registros en el banco del monopoly.

package structures;

public class DoublyLinkedList<T> {
    private DoubleNode<T> head;
    private DoubleNode<T> tail;

    public DoublyLinkedList() {
        head = null;
        tail = null;
    }

    public void addTransaction(T transaction) {
        DoubleNode<T> newNode = new DoubleNode<>(transaction);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            newNode.setPrev(tail);
            tail = newNode;
        }
    }

    public DoubleNode<T> getOldest() { // Traverse forwards
        return head;
    }

    public DoubleNode<T> getNewest() { // Traverse backwards
        return tail;
    }
    
}
