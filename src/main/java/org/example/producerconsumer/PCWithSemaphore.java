package org.example.producerconsumer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

class Buffer
{
    private final Queue<Integer> queue;
    static Semaphore semCon;
    static Semaphore semProd;

    public Buffer(int capacity) {
        this.queue = new LinkedList<>();
        semCon = new Semaphore(0);
        semProd = new Semaphore(capacity);
    }

    void get() {
        try {
            semCon.acquire();
            synchronized (this)
            {
                int item = queue.poll();
                System.out.println("Consumer consumed item " + item);
            }
            semProd.release();

        } catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }
    }

    void put(int item) {
        try {
            semProd.acquire();
            synchronized (this)
            {
                queue.add(item);
                System.out.println("Producer produced item " + item);
            }
            semCon.release();
        } catch (InterruptedException e) {
            System.out.println("Producer thread interrupted");
        }
    }
}
class ProdSemaphore implements Runnable{
    private final Buffer buffer;
    private static int item=0;
    ProdSemaphore(Buffer buffer)
    {
        this.buffer = buffer;
        new Thread(this,"Producer-"+(int)(Math.random()*100)).start();
    }
    @Override
    public void run() {
        while(true)
        {
            synchronized (ProdSemaphore.class) {
                buffer.put(item++);
            }
            try{
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                System.out.println("Thread interrupted -> producer");
            }
        }
    }
}
class ConsumerSemaphore implements Runnable
{
    Buffer buffer;
    ConsumerSemaphore(Buffer buffer)
    {
        this.buffer = buffer;
        new Thread(this,"Consumer-"+(int)(Math.random()*100)).start();
    }
    @Override
    public void run() {
        while(true)
        {
            buffer.get();
            try{
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                System.out.println("Thread interrupted -> consumer");
            }
        }
    }
}
public class PCWithSemaphore {
    public static void main(String[] args)
    {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter the no of producers: ");
        int producerCount = sc.nextInt();

        System.out.print("Enter the no of consumers: ");
        int consumerCount = sc.nextInt();

        System.out.print("Enter the capacity of Buffer: ");
        int capacity = sc.nextInt();

        Buffer buffer = new Buffer(capacity);

        for (int i = 0; i < producerCount; i++) {
            new ProdSemaphore(buffer);
        }

        for (int i = 0; i < consumerCount; i++) {
            new ConsumerSemaphore(buffer);
        }

    }
}
