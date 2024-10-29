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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TwoLockBlockingQueue<T> implements Serializable {
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
    private final Lock fileLock = new ReentrantLock();
    private final Condition producerNotFull = producerLock.newCondition();
    private final Condition consumerNotEmpty = consumerLock.newCondition();
    private final Condition fileNotEmpty = fileLock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array_two.ser");

    public TwoLockBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
        this.scheduler = Executors.newScheduledThreadPool(2,runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        startDaemonThreads();
    }

    public void produce(T item) {
        long start = System.nanoTime();
        producerLock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue is full, signaling serialization daemon.");
                producerNotFull.await();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            long end = System.nanoTime();
            System.out.println("Time taken to produce an item = "+(end-start));
            System.out.println("Produced item: " + item);
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
                System.out.println("Consumer queue is empty, signaling deserialization daemon.");
                consumerNotEmpty.await();
            }
            T item = consumerQueue[consumerFront];
            consumerQueue[consumerFront] = null;
            consumerFront = (consumerFront + 1) % capacity;
            consumerSize--;
            long end = System.nanoTime();
            System.out.println("Time taken to consume an item = "+(end-start));
            System.out.println("Consumed item: " + item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            consumerLock.unlock();
        }
    }

    private void serializeProducerQueueToFile() {
        fileLock.lock();
        try {
            T[] itemsToSerialize = Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize);
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                oos.writeObject(itemsToSerialize);
            }
            producerFront = 0;
            producerRear = 0;
            producerSize = 0;
            System.out.println("Serialized producer queue to file.");
            fileNotEmpty.signalAll();
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
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Error accessing the file: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private void startDaemonThreads() {
        scheduler.scheduleWithFixedDelay(()->{
                producerLock.lock();
                try {
                    if (producerSize == capacity) {
                        serializeProducerQueueToFile();
                        producerNotFull.signalAll();
                    }
                } finally {
                    producerLock.unlock();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },0,1, TimeUnit.SECONDS);


        scheduler.scheduleWithFixedDelay(()->{
                consumerLock.lock();
                try {
                    if (consumerSize == 0) {
                        deserializeToConsumerQueueFromFile();
                        consumerNotEmpty.signalAll();
                    }
                } finally {
                    consumerLock.unlock();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },0,1,TimeUnit.SECONDS);

    }

    public void viewSerializedData() {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
            Object[] deserializedArray = (Object[]) ois.readObject();
            System.out.println("Deserialized contents of the file:");
            System.out.println(Arrays.toString(deserializedArray));
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during deserialization: " + e.getMessage());
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


class TwoLockProducerImpl implements Runnable
{
    private final TwoLockBlockingQueue<Integer> producerQueue;
    private final int id;

    public TwoLockProducerImpl(TwoLockBlockingQueue<Integer> queue, int id) {
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
class TwoLockConsumerImpl implements Runnable
{
    private final TwoLockBlockingQueue<Integer> consumerQueue;

    public TwoLockConsumerImpl(TwoLockBlockingQueue<Integer> queue) {
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
class TwoLockMain
{
    public static void main(String[] args) {
        System.out.println("Application start time "+System.nanoTime());

        TwoLockBlockingQueue<Integer> blockingQueue = new TwoLockBlockingQueue<>(3);

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(new TwoLockProducerImpl(blockingQueue,1));
        threadPool.submit(new TwoLockConsumerImpl(blockingQueue));
        threadPool.shutdown();
        blockingQueue.shutdownService();

    }
}
