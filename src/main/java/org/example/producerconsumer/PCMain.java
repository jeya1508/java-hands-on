package org.example.producerconsumer;

import java.util.Scanner;

public class PCMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of producers: ");
        int numProducers = scanner.nextInt();

        System.out.print("Enter number of consumers: ");
        int numConsumers = scanner.nextInt();

        System.out.print("Enter the capacity: ");
        int capacity = scanner.nextInt();

        SharedMemory buffer = new SharedMemory(capacity);
        for (int i = 1; i <= numProducers; i++) {
            Thread producer = new Thread(new Producer(buffer, i));
            producer.start();
        }

        for (int i = 1; i <= numConsumers; i++) {
            Thread consumer = new Thread(new Consumer(buffer, i));
            consumer.start();
        }

        scanner.close();
    }
}
