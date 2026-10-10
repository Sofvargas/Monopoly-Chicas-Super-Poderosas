// Board 
// Implementacion de una lista circular doblemente enlazada. 
// diseñada especificamente para modelar el tablero de monopoly de forma continua 
// permitiendo avanzar y retroceder entre casillas de manera infinita sin desbordamientos
package structures;

public class CircularDoublyLinkedList<T> {
    private DoubleNode<T> head;
    private int size;

    public CircularDoublyLinkedList() {
        this.head = null;
        this.size = 0;
    }

    public void insert(T data) {
        DoubleNode<T> newNode = new DoubleNode<>(data);
        if (head == null) {
            head = newNode;
            head.setNext(head);
            head.setPrev(head);
        } else {
            DoubleNode<T> tail = head.getPrev();
            tail.setNext(newNode);
            newNode.setPrev(tail);
            newNode.setNext(head);
            head.setPrev(newNode);
        }
        size++;
    }

    public DoubleNode<T> movePositions(DoubleNode<T> node, int positions) {
        if (node == null) return null;
        DoubleNode<T> temp = node;
        for (int i = 0; i < positions; i++) {
            temp = temp.getNext();
        }
        return temp;
    }
    public int getSize() {
        return size;
    }
    public DoubleNode<T> getHead() {
        return head;
    }
}

