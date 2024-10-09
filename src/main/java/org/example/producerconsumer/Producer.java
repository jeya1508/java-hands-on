package org.example.producerconsumer;

public class Producer implements Runnable{
    private final SharedMemory memory;
    private final int id;

    public Producer(SharedMemory memory,int id)
    {
        this.memory = memory;
        this.id = id;
    }
    @Override
    public void run() {
        try{
            int item = 0;
            while (true) {
                memory.produce(item);
                    System.out.println("Producer " + id + " produced: " + item);
                item++;
                Thread.sleep(500);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
