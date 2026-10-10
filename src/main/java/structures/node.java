// Node Classes
// Clase generica que representa un nodo basico para estructuras enlazadas simples
// Este va a almacenar un valor de tipo generico (T) y una referencia al siguiente de nodo.
package structures;

public class node<T> { // Singly Linked List and Queues
    private T data;
    private node<T> next;

    public node(T data) {
        this.data = data;
        this.next = null;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public node<T> getNext() {
        return next;
    }

    public void setNext(node<T> next) {
        this.next = next;
    }
    
}


