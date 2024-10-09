package org.example.threading;
class SharedResource
{
    private int value;
    private boolean available = false;
    public synchronized void set(int value)
    {
        while(available)
        {
            try{
                wait(); //If any thread is available and using the resource, then wait until that thread completes the task
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        this.value = value;
        this.available = true;
        System.out.println("Value set is "+value);
        notify();
    }
    public synchronized void get()
    {
        while(!available)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        this.available = false;
        System.out.println("Value get is "+value);
        notify();
    }
}
public class InterThreadCommExample {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();
        new Thread(()->{
            for (int i = 0; i < 5; i++) {
                resource.set(i);
            }
        }).start();

        new Thread(()->{
            for (int i = 0; i < 5; i++) {
                resource.get();
            }
        }).start();
    }
}
