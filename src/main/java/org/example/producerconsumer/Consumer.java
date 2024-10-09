package org.example.producerconsumer;


public class Consumer implements Runnable{
    private final SharedMemory memory;
    private final int id;

    public Consumer(SharedMemory memory, int id)
    {
        this.memory = memory;
        this.id = id;
    }

    @Override
    public void run() {
        try{
            while(true){
                int item = memory.consume();
                System.out.println("Consumer " + id + " consumed item: " + item);
                Thread.sleep(1000);
            }
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
