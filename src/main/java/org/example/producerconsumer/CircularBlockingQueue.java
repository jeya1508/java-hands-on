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
    private final Condition notFull = lock.newCondition();

    private static final Path FILE_PATH = Paths.get("queue_data_array_one.ser");

    public CircularBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
    }

    public void produce(T item) {
        long start = System.nanoTime();
        lock.lock();
        try {
            if (producerSize == capacity) {
                System.out.println("Producer queue is full, signaling daemon for serialization.");
                notFull.signal();
                notFull.await();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;
            long end = System.nanoTime();
            System.out.println("Time taken to produce an item = "+(end-start));
            System.out.println("Produced item: " + item);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void consume() {
        long start = System.nanoTime();
        lock.lock();
        try {
            if (consumerSize == 0) {
                System.out.println("Consumer queue is empty, signaling daemon for deserialization.");
                notEmpty.await();
            }
            T item = consumerQueue[consumerFront];
            consumerQueue[consumerFront] = null;
            consumerFront = (consumerFront + 1) % capacity;
            consumerSize--;
            long end = System.nanoTime();
            System.out.println("Time taken to consume an item = "+(end-start));
            System.out.println("Consumed item: " + item);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void startSerializationDaemon() {
        Thread serializationThread = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (producerSize < capacity) {
                        notFull.await();
                    }
                    System.out.println("Into the serialization daemon thread");
                    serializeProducerQueueToFile();
                    notEmpty.signalAll();
                    notFull.signal();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        });
        serializationThread.setDaemon(true); 
        serializationThread.start();
    }

    public void startDeserializationDaemon() {
        Thread deserializationThread = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (consumerSize > 0 || Files.size(FILE_PATH) == 0) {
                        notEmpty.await();
                    }
                    deserializeToConsumerQueueFromFile();
                    Thread.sleep(1000);
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        });
        deserializationThread.setDaemon(true);
        deserializationThread.start();
    }

    private void serializeProducerQueueToFile() {
        lock.lock();
        try {
            T[] newArr = Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize);
            System.out.println("Array to be serialized " + Arrays.toString(newArr));

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                oos.writeObject(newArr);
            }

            producerFront = 0;
            producerRear = 0;
            producerSize = 0;

            System.out.println("Serialized producer queue to file.");
            viewSerializedData();
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
//            producerQueue.viewSerializedData();
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
//                consumerQueue.viewSerializedData();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}

class Main {
    public static void main(String[] args) {
        System.out.println("Start of application "+System.nanoTime());

        CircularBlockingQueue<Integer> queue = new CircularBlockingQueue<>(3);
        queue.startSerializationDaemon();
        queue.startDeserializationDaemon();
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        threadPool.submit(new ProducerImpl(queue, 1));
        threadPool.submit(new ConsumerImpl(queue));
        threadPool.shutdown();
    }
}