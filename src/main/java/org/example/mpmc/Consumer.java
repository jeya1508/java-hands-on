package org.example.mpmc;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
class Consumer<T> implements Runnable{
    private final CircularArrayBlockingQueue<T> queue;
    private final FileStore<T> fileStore = new FileStore<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Producer<T> producer;

    public Consumer(CircularArrayBlockingQueue<T> queue, Producer<T> producer) {
        this.queue = queue;
        this.producer = producer;
        startDeserializationDaemon();
    }
    public void run() {
        while (true) {
            T item = queue.consume();
            System.out.println("Consumed item: " + item);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startDeserializationDaemon() {
        scheduler.scheduleWithFixedDelay(() -> {
            synchronized (producer) {
                try {
                    while (!producer.isDataAvailableForConsumer()) {
                        producer.wait();
                    }
                    System.out.println("Consumer queue is "+ Arrays.toString(queue.queue));
                    if (Files.exists(FileStore.FILE_PATH) && Files.size(FileStore.FILE_PATH) > 0) {
                        T[] itemsToDeserialize = fileStore.deserialize();
                        if (itemsToDeserialize != null) {
                            for (T item : itemsToDeserialize) {
                                if (item != null){
                                    queue.produce(item);
                                }
                            }
                            System.out.println("Deserialized data loaded into the queue.");

                        }
                        producer.resetDataAvailability();
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}