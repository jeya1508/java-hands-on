package org.example.mpmc;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Producer<T> implements Runnable{
    private final CircularArrayBlockingQueue<T> queue;
    private T item;
    private final FileStore<T> fileStore = new FileStore<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean dataAvailableForConsumer = false;

    public Producer(CircularArrayBlockingQueue<T> queue,T item) {
        this.queue = queue;
        this.item = item;
        startSerializationDaemon();
    }

    public void run(){
        int counter = 0;
        while (true) {
            T finalItem = (T) (item.toString() + counter++);
            queue.produce(finalItem);
            System.out.println("Produced item: " + finalItem);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startSerializationDaemon() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (queue.isFull()) {
                System.out.println("Producer queue is "+Arrays.toString(queue.queue));
                T[] itemsToSerialize = Arrays.copyOf(queue.queue, queue.capacity);
                System.out.println("Queue is full. Serializing data to file.");
                fileStore.serialize(itemsToSerialize);

                dataAvailableForConsumer = true;
                synchronized (this) {
                    queue.signalConsumer();
                    notifyAll();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized boolean isDataAvailableForConsumer() {
        return dataAvailableForConsumer;
    }
    public synchronized void resetDataAvailability() {
        dataAvailableForConsumer = false;
    }
}

