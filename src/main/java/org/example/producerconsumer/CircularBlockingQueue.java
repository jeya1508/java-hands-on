package org.example.producerconsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CircularBlockingQueue<T> {
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

    private static final Path FILE_PATH = Paths.get("data_buffer.txt");

    public CircularBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];

        try {
            if (!Files.exists(FILE_PATH)) {
                Files.createFile(FILE_PATH);
            }
        } catch (IOException e) {
            System.out.println("Error in creating the file: " + e.getMessage());
        }
    }

    public void produce(T item) {
        lock.lock();
        try {
            while (producerSize == capacity) {
                System.out.println("Producer queue full, writing to file.");
                writeProducerQueueToFile();
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
                System.out.println("Consumer queue empty, waiting for data from file...");
                fillConsumerQueueFromFile();
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

    private void writeProducerQueueToFile() {
        lock.lock();
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (int i = 0; i < producerSize; i++) {
                int index = (producerFront + i) % capacity;
                ByteBuffer buffer = ByteBuffer.wrap((producerQueue[index].toString() + "\n").getBytes());
                fileChannel.write(buffer);
            }
            producerFront = 0;
            producerRear = 0;
            producerSize = 0;
            System.out.println("Producer queue contents written to file.");
        } catch (IOException e) {
            System.out.println("Error in writing contents to the file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void fillConsumerQueueFromFile() {
        lock.lock();
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = fileChannel.read(buffer);
            buffer.flip();

            if (bytesRead == -1) {
                return;
            }

            StringBuilder itemBuilder = new StringBuilder();
            while (buffer.hasRemaining()) {
                itemBuilder.append((char) buffer.get());
            }

            String[] lines = itemBuilder.toString().split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty() && consumerSize < capacity) {
                    T item = (T) Integer.valueOf(line.trim());
                    consumerQueue[consumerRear] = item;
                    consumerRear = (consumerRear + 1) % capacity;
                    consumerSize++;
                }
            }

            fileChannel.truncate(0);
            System.out.println("Filled consumer queue from file. Current consumer queue: " + Arrays.toString(consumerQueue));
        } catch (IOException e) {
            System.out.println("Error while reading from the file: " + e.getMessage());
        } finally {
            lock.unlock();
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

        for (int i = 1; i <= numProducers; i++) {
            Thread producer = new Thread(new ProducerImpl(queue, i));
            producer.start();
        }
        for (int i = 1; i <= numConsumers; i++) {
            Thread consumer = new Thread(new ConsumerImpl(queue));
            consumer.start();
        }
    }
}
