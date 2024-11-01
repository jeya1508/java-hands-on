package org.example.mpmc;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircularArrayBlockingQueue<T>{
    final T[] queue;
    final int capacity;
    private int front = 0, rear = 0, size = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    @SuppressWarnings("unchecked")
    public CircularArrayBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.queue = (T[]) new Object[capacity];
    }

    public void produce(T item) {
        lock.lock();
        try {
            while (size == capacity) {
                notFull.await();
            }
            queue[rear] = item;
            rear = (rear + 1) % capacity;
            size++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public T consume() {
        lock.lock();
        try {
            while (size == 0) {
                notEmpty.await();
            }
            T item = queue[front];
            queue[front] = null;
            front = (front + 1) % capacity;
            size--;
            notFull.signal();
            return item;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.unlock();
        }
    }

    public boolean isFull() {
        return size == capacity;
    }

    public boolean isEmpty() {
        return size == 0;
    }
    public void signalConsumer()
    {
        lock.lock();
        try{
            notEmpty.signalAll();
        }
        finally {
            lock.unlock();
        }
    }
}
