package structures;

public class SinglyLinkedList<T> {
    private node<T> head;

    public SinglyLinkedList() {
        head = null;
    }

    public void add(T data) {
        node<T> newNode = new node<>(data);
        if (head == null) {
            head = newNode;
        } else {
            node<T> temp = head;
            while (temp.getNext() != null) {
                temp = temp.getNext();
            }
            temp.setNext(newNode);
        }
    }

    public void removeProperty(T data) {
        if (head == null) return;
        if (head.getData().equals(data)) {
            head = head.getNext();
            return;
        }
        node<T> temp = head;
        while (temp.getNext() != null && !temp.getNext().getData().equals(data)) {
            temp = temp.getNext();
        }
        if (temp.getNext() != null) {
            temp.setNext(temp.getNext().getNext());
        }
    }

    public node<T> getHead() { return head; }
}
