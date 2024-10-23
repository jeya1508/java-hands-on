package org.example.serialization;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBlockQueue<T> implements Externalizable {
    private static final long serialVersionUID = 254L;

    private T[] producerQueue;
    private T[] consumerQueue;
    private int capacity;

    private int producerFront = 0;
    private int producerRear = 0;
    private int producerSize = 0;

    private int consumerFront = 0;
    private int consumerRear = 0;
    private int consumerSize = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array.ser");

    public CircularBlockQueue() {
    }

    public CircularBlockQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(producerSize);
        for (int i = 0; i < producerSize; i++) {
            out.writeObject(producerQueue[(producerFront + i) % capacity]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        producerSize = size;

        for (int i = 0; i < size; i++) {
            producerQueue[(producerFront + i) % capacity] = (T) in.readObject();
        }
    }

    private void serializeProducerQueueToFile() {
        lock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            for (int i = 0; i < producerSize; i++) {
                oos.writeObject(producerQueue[(producerFront + i) % capacity]);
            }
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
        try {
            if (Files.exists(FILE_PATH) && Files.size(FILE_PATH) > 0) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
                    consumerFront = 0;
                    consumerRear = 0;
                    consumerSize = 0;

                    while (true) {
                        T item = (T) ois.readObject();
                        consumerQueue[consumerRear] = item;
                        consumerRear = (consumerRear + 1) % capacity;
                        consumerSize++;
                    }
                } catch (EOFException e) {
                    System.out.println("Finished deserializing items from file.");
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("The file is empty or does not exist, no data to deserialize.");
            }
        } catch (IOException e) {
            System.out.println("Error accessing the file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void produce(T item) {
        long start = System.nanoTime();
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
            long end = System.nanoTime();
            System.out.println("Produced item: " + item + ", Time taken: " + (end - start) + " nanoseconds");
        } finally {
            lock.unlock();
        }
    }

    public void consume() {
        long start = System.nanoTime();
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
            long end = System.nanoTime();
            System.out.println("Consumed item: " + item + ", Time taken: " + (end - start) + " nanoseconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
}
class ProducerBlockQueue implements Runnable
{
    private final CircularBlockQueue<Integer> producerQueue;
    private final int id;
    public ProducerBlockQueue(CircularBlockQueue<Integer> queue, int id) {
        this.producerQueue = queue;
        this.id = id;
    }

    @Override
    public void run() {
        int item = 0;
        while (true) {
            item++;
            producerQueue.produce(item + id * 100);
//            producerQueue.viewSerializedData();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
class ConsumerBlockQueue implements Runnable
{
    private final CircularBlockQueue<Integer> consumerQueue;
    public ConsumerBlockQueue(CircularBlockQueue<Integer> queue) {
        this.consumerQueue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                consumerQueue.consume();
//                consumerQueue.viewSerializedData();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
class CircularQueueMain
{
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of producers: ");
        int numProducers = sc.nextInt();

        System.out.print("Enter number of consumers: ");
        int numConsumers = sc.nextInt();

        System.out.print("Enter the capacity for queues: ");
        int capacity = sc.nextInt();

        CircularBlockQueue<Integer> queue = new CircularBlockQueue<>(capacity);

        int totalThreads = numProducers + numConsumers;
        ExecutorService threadPool = Executors.newFixedThreadPool(totalThreads);

        for (int i = 1; i <= numProducers; i++) {
            threadPool.submit(new ProducerBlockQueue(queue, i));
        }
        for (int i = 1; i <= numConsumers; i++) {
            threadPool.submit(new ConsumerBlockQueue(queue));
        }
        threadPool.shutdown();
    }
}
