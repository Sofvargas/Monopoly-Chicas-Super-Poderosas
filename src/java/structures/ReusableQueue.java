package structures;

public class ReusableQueue<T> {
    private node<T> front;
    private node<T> rear;

    public ReusableQueue() {
        front = null;
        rear = null;
    }

    public void enqueue(T card) {
        node<T> newNode = new node<>(card);
        if (rear == null) {
            front = newNode;
            rear = newNode;
        } else {
            rear.setNext(newNode);
            rear = newNode;
        }
    }

    public T dequeue() {
        if (front == null) return null;
        T card = front.getData();
        front = front.getNext();
        if (front == null) {
            rear = null;
        }
        return card;
    }

    // Draws the top card and puts it at the end of the queue (reusable)
    public T drawAndReuse() {
        T card = dequeue();
        if (card != null) {
            enqueue(card); 
        }
        return card;
    }
}
    

