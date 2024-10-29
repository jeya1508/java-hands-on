package org.example.producerconsumer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockTwoLockQueue<T> implements Serializable {
    private static final long serialVersionUID = 12L;
    private final ScheduledExecutorService scheduler;
    private final T[] producerQueue;
    private final T[] consumerQueue;
    private final int capacity;

    private int producerFront = 0;
    private int producerRear = 0;
    private int producerSize = 0;

    private int consumerFront = 0;
    private int consumerRear = 0;
    private int consumerSize = 0;

    private final Lock producerLock = new ReentrantLock();
    private final Lock consumerLock = new ReentrantLock();
    private final Condition notEmptyConsumer = consumerLock.newCondition();
    private final Condition notFullProducer = producerLock.newCondition();

    private static final Path OVERFLOW_FILE = Paths.get("queue_data_array_two_nofile.ser");

    public ArrayBlockTwoLockQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];

        this.scheduler = Executors.newScheduledThreadPool(2, runnable ->{
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        startSerializationThread();
        startDeserializationThread();
    }

    public void produce(T item) {
        long start = System.nanoTime();
        producerLock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue full, waiting...");
                notFullProducer.await();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            long end = System.nanoTime();
            System.out.println("Time taken to produce an item "+(end-start));
            System.out.println("Produced item: " + item);
            signalConsumer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            producerLock.unlock();
        }
    }

    public void consume() {
        long start = System.nanoTime();
        consumerLock.lock();
        try {
            while (consumerSize == 0) {
                System.out.println("Consumer queue empty, waiting for items...");
                notEmptyConsumer.await();
            }
            T item = consumerQueue[consumerFront];
            consumerQueue[consumerFront] = null;
            consumerFront = (consumerFront + 1) % capacity;
            consumerSize--;
            long end = System.nanoTime();
            System.out.println("Time taken to consume an item "+(end-start));
            System.out.println("Consumed item: " + item);
            signalProducer();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            consumerLock.unlock();
        }
    }

    private void startSerializationThread() {
        scheduler.scheduleWithFixedDelay(() -> {
                producerLock.lock();
                try {
                    if (producerSize == capacity) {
                        serializeProducerQueueToFile();
                    }
                } finally {
                    producerLock.unlock();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },0, 1, TimeUnit.SECONDS);

        }

    private void startDeserializationThread() {
        scheduler.scheduleWithFixedDelay(() -> {
                consumerLock.lock();
                try {
                    if (consumerSize < capacity) {
                        deserializeFromFile();
                    }
                } finally {
                    consumerLock.unlock();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },0, 1, TimeUnit.SECONDS);
    }

    private void serializeProducerQueueToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(OVERFLOW_FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            while (producerSize > 0) {
                T item = producerQueue[producerFront];
                producerQueue[producerFront] = null;
                producerFront = (producerFront + 1) % capacity;
                producerSize--;
                oos.writeObject(item);
            }
            System.out.println("Serialized producer queue to file.");
            signalProducer();
        } catch (IOException e) {
            System.out.println("Error during serialization: " + e.getMessage());
        }
    }

    private void deserializeFromFile() {
        try {
            if (Files.exists(OVERFLOW_FILE) && Files.size(OVERFLOW_FILE) > 0) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(OVERFLOW_FILE))) {
                    int availableSpace = capacity - consumerSize;
                    for (int i = 0; i < availableSpace; i++) {
                        T item = (T) ois.readObject();
                        consumerQueue[consumerRear] = item;
                        consumerRear = (consumerRear + 1) % capacity;
                        consumerSize++;
                        System.out.println("Deserialized item to consumer queue: " + item);
                    }
                    signalConsumer();

                    Files.newOutputStream(OVERFLOW_FILE, StandardOpenOption.TRUNCATE_EXISTING).close();
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("Error during deserialization: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error accessing the overflow file: " + e.getMessage());
        }
    }

    private void signalConsumer() {
        consumerLock.lock();
        try {
            notEmptyConsumer.signal();
        } finally {
            consumerLock.unlock();
        }
    }

    private void signalProducer() {
        producerLock.lock();
        try {
            notFullProducer.signal();
        } finally {
            producerLock.unlock();
        }
    }

    public void shutdownService() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
class ArrayBlockTwoLockProducer implements Runnable
{
    private final ArrayBlockTwoLockQueue<Integer> producerQueue;
    private final int id;

    public ArrayBlockTwoLockProducer(ArrayBlockTwoLockQueue<Integer> queue, int id) {
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
class ArrayBlockTwoLockConsumer implements Runnable
{
    private final ArrayBlockTwoLockQueue<Integer> consumerQueue;

    public ArrayBlockTwoLockConsumer(ArrayBlockTwoLockQueue<Integer> queue) {
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
class ArrayBlockTwoLockMain
{
    public static void main(String[] args) {
        long start = System.nanoTime();
        System.out.println("Start of application is "+start);

        ArrayBlockTwoLockQueue<Integer> blockingQueue = new ArrayBlockTwoLockQueue<>(3);
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        threadPool.submit(new ArrayBlockTwoLockProducer(blockingQueue,1));
        threadPool.submit(new ArrayBlockTwoLockConsumer(blockingQueue));
        threadPool.shutdown();
        blockingQueue.shutdownService();

    }
}
