// Node Classes
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


