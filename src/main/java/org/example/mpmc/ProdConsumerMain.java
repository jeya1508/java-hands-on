package org.example.mpmc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProdConsumerMain {
    public static void main(String[] args) {
        CircularArrayBlockingQueue<Integer> queue = new CircularArrayBlockingQueue<>(3);
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        Producer<Integer> producer = new Producer<>(queue,1);
        Consumer<Integer> consumer = new Consumer<>(queue,producer);
        threadPool.submit(producer);
        threadPool.submit(consumer);
        threadPool.shutdown();

    }
}