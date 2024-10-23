package org.example.producerconsumer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockQueue<T> implements Serializable {
    private static final long serialVersionUID = 123L;

    private final T[] producerQueue;
    private final T[] consumerQueue;
    private final int capacity;

    private int producerFront = 0;
    private int producerRear = 0;
    private int producerSize = 0;

    private int consumerFront = 0;
    private int consumerRear = 0;
    private int consumerSize = 0;

    private final Lock queueLock = new ReentrantLock();
    private final Lock fileLock = new ReentrantLock();
    private final Condition notEmpty = queueLock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array_block.ser");

    public ArrayBlockQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
    }

    public void produce(T item) {
        long start = System.nanoTime();
        queueLock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue full, serializing to file.");
                queueLock.unlock();
                serializeProducerQueueToFile();
                queueLock.lock();
                notEmpty.signal();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            long end = System.nanoTime();
            System.out.println("Produced item: " + item + ", Time taken: " + (end - start) + " nanoseconds");
        } finally {
            queueLock.unlock();
        }
    }

    public void consume() {
        long start = System.nanoTime();
        queueLock.lock();
        try {
            while (consumerSize == 0) {
                System.out.println("Consumer queue empty, deserializing from file...");
                queueLock.unlock();
                deserializeToConsumerQueueFromFile();
                queueLock.lock();

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
            queueLock.unlock();
        }
    }


    private void serializeProducerQueueToFile() {
        fileLock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            oos.writeObject(Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize));
            producerFront = 0;
            producerRear = 0;
            producerSize = 0;
            System.out.println("Serialized producer queue to file.");
        } catch (IOException e) {
            System.out.println("Error during serialization: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }


    private void deserializeToConsumerQueueFromFile() {
        fileLock.lock();
        try {
            if (Files.exists(FILE_PATH) && Files.size(FILE_PATH) > 0) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
                    T[] deserializedArray = (T[]) ois.readObject();

                    if (deserializedArray.length > 0) {
                        int toConsume = Math.min(capacity - consumerSize, deserializedArray.length);
                        for (int i = 0; i < toConsume; i++) {
                            consumerQueue[consumerRear] = deserializedArray[i];
                            consumerRear = (consumerRear + 1) % capacity;
                            consumerSize++;
                        }
                        if (toConsume < deserializedArray.length) {
                            T[] remainingItems = Arrays.copyOfRange(deserializedArray, toConsume, deserializedArray.length);
                            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                                oos.writeObject(remainingItems);

                            }
                        } else {
                            Files.newOutputStream(FILE_PATH, StandardOpenOption.TRUNCATE_EXISTING).close();

                        }
                    } else {
                        System.out.println("No items to consume");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("The file is empty or does not exist, no data to deserialize.");
            }
        } catch (IOException e) {
            System.out.println("Error accessing the file: "+ e.getMessage());
        } finally {
            fileLock.unlock();
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
class ArrayBlockProducer implements Runnable
{
    private final ArrayBlockQueue<Integer> producerQueue;
    private final int id;

    public ArrayBlockProducer(ArrayBlockQueue<Integer> queue, int id) {
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
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
class ArrayBlockConsumer implements Runnable
{
    private final ArrayBlockQueue<Integer> consumerQueue;

    public ArrayBlockConsumer(ArrayBlockQueue<Integer> queue) {
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
class ArrayBlockMain
{
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of producers: ");
        int numProducers = scanner.nextInt();

        System.out.print("Enter number of consumers: ");
        int numConsumers = scanner.nextInt();

        System.out.print("Enter the capacity for queues: ");
        int capacity = scanner.nextInt();

        ArrayBlockQueue<Integer> arrayBlockQueue = new ArrayBlockQueue<>(capacity);
        int totalThreads = numProducers + numConsumers;
        ExecutorService threadPool = Executors.newFixedThreadPool(totalThreads);

        for (int i = 1; i <= numProducers; i++) {
            threadPool.submit(new ArrayBlockProducer(arrayBlockQueue,i));
        }
        for (int i = 1; i <= numConsumers; i++) {
            threadPool.submit(new ArrayBlockConsumer(arrayBlockQueue));
        }
        threadPool.shutdown();
    }
}
