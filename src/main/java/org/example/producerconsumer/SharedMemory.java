package org.example.producerconsumer;

import java.util.LinkedList;
import java.util.Queue;

public class SharedMemory {
    private final Queue<Integer> queue;
    private final int capacity;

    public SharedMemory(int capacity)
    {
        queue = new LinkedList<>();
        this.capacity = capacity;
    }
    public synchronized void produce(int item) throws InterruptedException
    {
        while(queue.size()==capacity)
        {
            System.out.println("Buffer is full. Waiting for consumer to consume");
            wait();
        }
        queue.add(item);
        System.out.println("Item "+item+" added");
        System.out.println("Queue: "+queue);
        notifyAll();
    }
    public synchronized int consume() throws InterruptedException
    {
        while(queue.isEmpty())
        {
            System.out.println("No items in the buffer. Waiting for the producer to produce");
            wait();
        }
        int item = queue.poll();
        System.out.println("Item "+item+" consumed");
        System.out.println("Queue: "+queue);
        notifyAll();
        return item;
    }
}
