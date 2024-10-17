package org.example.producerconsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
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

    private static final Path FILE_PATH = Paths.get("data_buffer.txt");
    private static final int FILE_SIZE_LIMIT = 1024;

    public CircularBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.queue = (T[]) new Object[capacity];

        try {
            if (!Files.exists(FILE_PATH)) {
                Files.createFile(FILE_PATH);
            }
        } catch (IOException e) {
            System.out.println("Error in creating the file "+e.getMessage());
        }
    }

    public void produce(T item) {
        lock.lock();
        try {
            if (size < capacity) {
                queue[rear] = item;
                rear = (rear + 1) % capacity;
                size++;
            } else {
                T oldItem = queue[front];
                writeToFile(oldItem);
                queue[front] = item;
                front = (front + 1) % capacity;
            }
            System.out.println("Produced item: " + item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public void consume() throws InterruptedException {
        lock.lock();
        try {
            while (size == 0) {
                if (!isFileEmpty()) {
                    T item = readFromFile();
                    System.out.println("Consumed from file: " + item);
                } else {
                    notEmpty.await();
                }
            }
            if (size > 0) {
                T item = queue[front];
                front = (front + 1) % capacity;
                size--;
                System.out.println("Consumed from queue: " + item);
            }
        } finally {
            lock.unlock();
        }
    }
    private void writeToFile(T item) {
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put((item.toString() + "\n").getBytes());
            buffer.flip();

            long fileSize = fileChannel.size();
            if (fileSize > FILE_SIZE_LIMIT) {
                fileChannel.position(0);
            } else {
                fileChannel.position(fileSize);
            }
            fileChannel.write(buffer);
        } catch (IOException e) {
            System.out.println("Error in writing contents to the file "+e.getMessage());
        }
    }
    private T readFromFile() {
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            fileChannel.read(buffer);
            buffer.flip();

            StringBuilder itemBuilder = new StringBuilder();
            while (buffer.hasRemaining()) {
                itemBuilder.append((char) buffer.get());
            }

            String fileContent = itemBuilder.toString();
            String[] lines = fileContent.split("\n");

            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                T item = (T) Integer.valueOf(firstLine);

                StringBuilder remainingContent = new StringBuilder();
                for (int i = 1; i < lines.length; i++) {
                    remainingContent.append(lines[i]).append("\n");
                }

                ByteBuffer remainingBuffer = ByteBuffer.wrap(remainingContent.toString().getBytes());
                fileChannel.truncate(0);
                fileChannel.write(remainingBuffer);

                return item;
            }
        } catch (IOException e) {
            System.out.println("Error while reading from the file "+e.getMessage());
        }
        return null;
    }

    private boolean isFileEmpty() {
        try {
            return Files.size(FILE_PATH) == 0;
        } catch (IOException e) {
            System.out.println("Error while finding the file size "+e.getMessage());
        }
        return true;
    }
}

class ProducerImpl implements Runnable {
    private final CircularBlockingQueue<Integer> queue;
    private final int id;

    public ProducerImpl(CircularBlockingQueue<Integer> queue, int id) {
        this.queue = queue;
        this.id = id;
    }

    @Override
    public void run() {
        int item = 0;
        while (true) {
            item++;
            queue.produce(item + id * 100);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ConsumerImpl implements Runnable {
    private final CircularBlockingQueue<Integer> queue;

    public ConsumerImpl(CircularBlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                queue.consume();
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

        System.out.print("Enter the capacity: ");
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
