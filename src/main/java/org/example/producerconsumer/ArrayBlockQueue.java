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
    private final Condition notFull = queueLock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array_block.ser");

    public ArrayBlockQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];

        startSerializationDaemon();
        startDeserializationDaemon();
    }

    public void produce(T item) {
        long start = System.nanoTime();
        queueLock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue full, signaling daemon for serialization.");
                notFull.signal();
                notFull.await();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            notEmpty.signal();
            long end = System.nanoTime();
            System.out.println("Time taken to produce an item = "+(end-start));
            System.out.println("Produced item "+item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            queueLock.unlock();
        }
    }

    public void consume() {
        long start = System.nanoTime();
        queueLock.lock();
        try {
            while (consumerSize == 0) {
                System.out.println("Consumer queue empty, waiting for deserialization...");
                notEmpty.await();
            }
            T item = consumerQueue[consumerFront];
            consumerQueue[consumerFront] = null;
            consumerFront = (consumerFront + 1) % capacity;
            consumerSize--;
            notFull.signal();
            long end = System.nanoTime();
            System.out.println("Time taken to consume an item = "+(end-start));
            System.out.println("Consumed item: " + item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            queueLock.unlock();
        }
    }

    private void startSerializationDaemon() {
        Thread serializationThread = new Thread(() -> {
            while (true) {
                queueLock.lock();
                try {
                    while (producerSize < capacity) {
                        notFull.await();
                    }
                    serializeProducerQueueToFile();
                    notEmpty.signalAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    queueLock.unlock();
                }
            }
        });
        serializationThread.setDaemon(true);
        serializationThread.start();
    }

    private void startDeserializationDaemon() {
        Thread deserializationThread = new Thread(() -> {
            while (true) {
                queueLock.lock();
                try {
                    while (consumerSize > 0 || !Files.exists(FILE_PATH) || Files.size(FILE_PATH) == 0) {
                        notEmpty.await();
                    }
                    deserializeToConsumerQueueFromFile();
                    Thread.sleep(1000);
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    queueLock.unlock();
                }
            }
        });
        deserializationThread.setDaemon(true);
        deserializationThread.start();
    }

    private void serializeProducerQueueToFile() {
        fileLock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            T[] itemsToSerialize = Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize);
            oos.writeObject(itemsToSerialize);
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
                    System.out.println("Deserialized Array "+Arrays.toString(deserializedArray));
                    if (deserializedArray.length > 0) {
                        int toConsume = Math.min(capacity - consumerSize, deserializedArray.length);
                        for (int i = 0; i < toConsume; i++) {
                            consumerQueue[consumerRear] = deserializedArray[i];
                            consumerRear = (consumerRear + 1) % capacity;
                            consumerSize++;
                        }
                        System.out.println("Consumer queue is "+ Arrays.toString(consumerQueue));
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
        System.out.println("Application start time "+System.nanoTime());
        Scanner scanner = new Scanner(System.in);
        ArrayBlockQueue<Integer> arrayBlockQueue = new ArrayBlockQueue<>(3);
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        threadPool.submit(new ArrayBlockProducer(arrayBlockQueue,1));
        threadPool.submit(new ArrayBlockConsumer(arrayBlockQueue));
        threadPool.shutdown();
    }
}
