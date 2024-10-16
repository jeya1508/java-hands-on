package org.example.producerconsumer;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CircularBlockingQueue<T> {
    private final T[] queue;
    private final int capacity;
    private int front = 0;
    private int rear = 0;
    private int size = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public CircularBlockingQueue(int capacity)
    {
        this.capacity = capacity;
        this.queue = (T[]) new Object[capacity];
    }

    public void produce(T item)
    {
        lock.lock();
        try{
            queue[rear] = item;
            rear = (rear+1) % capacity;
            if(size<capacity)
            {
                size++;
            }
            else{
                front = (front+1) % capacity;
            }
            System.out.println("Produced item: "+item);
            notEmpty.signal();
        }
        finally {
            lock.unlock();
        }
    }

    public void consume() throws InterruptedException
    {
        lock.lock();
        try {
            while(size == 0)
            {
                notEmpty.await();
            }
            T item = queue[front];
            front = (front+1) %capacity;
            size--;
            System.out.println("Consumed item: "+item);
        }
        finally {
            lock.unlock();
        }
    }
}

class ProducerImpl implements Runnable
{
    private final CircularBlockingQueue<Integer> queue;
    private final int id;

    public ProducerImpl(CircularBlockingQueue<Integer> queue, int id) {
        this.queue = queue;
        this.id = id;
    }

    @Override
    public void run()
    {
        int item = 0;
        while(true)
        {
            item++;
            queue.produce(item+id*100);
            try{
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
class ConsumerImpl implements Runnable
{
    private final CircularBlockingQueue<Integer> queue;
    public ConsumerImpl(CircularBlockingQueue<Integer> queue)
    {
        this.queue = queue;
    }
    @Override
    public void run()
    {
        while(true)
        {
            try{
                queue.consume();
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter number of producers: ");
        int numProducers = scanner.nextInt();

        System.out.print("Enter number of consumers: ");
        int numConsumers = scanner.nextInt();

        System.out.print("Enter the capacity: ");
        int capacity = scanner.nextInt();

        CircularBlockingQueue<Integer> queue = new CircularBlockingQueue<>(capacity);
        for (int i = 1; i <= numProducers; i++) {
            Thread producer = new Thread(new ProducerImpl(queue, i));
            producer.start();
        }
        for (int i = 1; i <= numConsumers ; i++) {
            Thread consumer = new Thread(new ConsumerImpl(queue));
            consumer.start();
        }

    }
}
