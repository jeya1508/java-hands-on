package org.example.producerconsumer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CircularBlockingQueue<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final T[] producerQueue;
    private final T[] consumerQueue;
    private final int capacity;

    private int producerFront = 0;
    private int producerRear = 0;
    private int producerSize = 0;

    private int consumerFront = 0;
    private int consumerRear = 0;
    private int consumerSize = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array.ser");

    public CircularBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
    }

    public void produce(T item) {
        lock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue full, serializing to file.");
                serializeProducerQueueToFile();
                notEmpty.signal();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            System.out.println("Produced item: " + item);
        } finally {
            lock.unlock();
        }
    }

    public void consume() {
        lock.lock();
        try {
            while (consumerSize == 0) {
                System.out.println("Consumer queue empty, deserializing from file...");
                deserializeToConsumerQueueFromFile();
                if (consumerSize == 0) {
                    notEmpty.await();
                }
            }
            T item = consumerQueue[consumerFront];
            consumerQueue[consumerFront] = null;
            consumerFront = (consumerFront + 1) % capacity;
            consumerSize--;
            System.out.println("Consumed item: " + item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void serializeProducerQueueToFile() {
        lock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            oos.writeObject(Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize));
            producerFront = 0;
            producerRear = 0;
            producerSize = 0;
            System.out.println("Serialized producer queue to file.");
        } catch (IOException e) {
            System.out.println("Error during serialization: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void deserializeToConsumerQueueFromFile() {
        lock.lock();
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
            T[] deserializedArray = (T[]) ois.readObject();
            for (int i = 0; i < deserializedArray.length && i < capacity; i++) {
                consumerQueue[consumerRear] = deserializedArray[i];
                consumerRear = (consumerRear + 1) % capacity;
                consumerSize++;
            }
            System.out.println("Deserialized consumer queue from file. Current consumer queue: " + Arrays.toString(consumerQueue));
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during deserialization: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
    public void viewSerializedData(){
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(FILE_PATH.toUri())))) {
            Object[] deserializedArray = (Object[]) ois.readObject();
            System.out.println("Deserialized contents of the file:");
            System.out.println(Arrays.toString(deserializedArray));
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during deserialization: " + e.getMessage());
        }
    }
}

class ProducerImpl implements Runnable {
    private final CircularBlockingQueue<Integer> producerQueue;
    private final int id;

    public ProducerImpl(CircularBlockingQueue<Integer> queue, int id) {
        this.producerQueue = queue;
        this.id = id;
    }

    @Override
    public void run() {
        int item = 0;
        while (true) {
            item++;
            producerQueue.produce(item + id * 100);
            producerQueue.viewSerializedData();
            try {
                Thread.sleep(500); // Simulate production time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ConsumerImpl implements Runnable {
    private final CircularBlockingQueue<Integer> consumerQueue;

    public ConsumerImpl(CircularBlockingQueue<Integer> queue) {
        this.consumerQueue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                consumerQueue.consume();
                consumerQueue.viewSerializedData();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
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

        System.out.print("Enter the capacity for queues: ");
        int capacity = scanner.nextInt();

        CircularBlockingQueue<Integer> queue = new CircularBlockingQueue<>(capacity);

        int totalThreads = numProducers + numConsumers;
        ExecutorService threadPool = Executors.newFixedThreadPool(totalThreads);

        for (int i = 1; i <= numProducers; i++) {
            threadPool.submit(new ProducerImpl(queue, i));
        }

        for (int i = 1; i <= numConsumers; i++) {
            threadPool.submit(new ConsumerImpl(queue));
        }

        threadPool.shutdown();
    }
}