package org.example.threading;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestLock {
    public static void main(String[] args)
    {
        TestResource test = new TestResource();
        Thread thread[] = new Thread[10];
        for (int i = 0; i < 5; i++)
        {
            thread[i] = new Thread(new DisplayJob(test),
                    "Thread " + i);
        }
        for (int i = 5; i < 10; i++)
        {
            thread[i] = new Thread(new ReadJob(test),
                    "Thread " + i);
        }
        for (int i = 0; i < 10; i++)
        {
            thread[i].start();
        }
    }
}
class DisplayJob implements Runnable
{
    private TestResource test;

    public DisplayJob(TestResource test) {
        this.test = test;
    }
    @Override
    public void run()
    {
        System.out.println("display job");
        test.displayRecord(new Object());
    }
}
class ReadJob implements Runnable
{
    private TestResource test;

    public ReadJob(TestResource test) {
        this.test = test;
    }
    @Override
    public void run()
    {
        System.out.println("read job");
        test.readRecord(new Object());
    }
}
class TestResource
{
    private final Lock displayQueueLock = new ReentrantLock();
    private final Lock readQueueLock = new ReentrantLock();
    public void displayRecord(Object document)
    {
        final Lock displayLock = this.displayQueueLock;
        displayLock.lock();
        try{
            long duration =
                    (long) (Math.random() * 10000);
            System.out.println(Thread.currentThread().getName()
                    + ": TestResource: display a Job"
                    + " during " + (duration / 1000) + " seconds ::"
                    + " Time - " + new Date());
            Thread.sleep(duration);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.printf("%s: The document has been"+
                    " displayed\n", Thread.currentThread().getName());
            displayLock.unlock();
        }
    }
    public void readRecord(Object document)
    {
        final Lock readLock = this.readQueueLock;
        readLock.lock();
        try{
            long duration = (long) (Math.random() * 10000);
            System.out.println(Thread.currentThread().getName() +
                    ": TestResource: read a Job"+
                    " during " + (duration / 1000) + " seconds ::"+
                    " Time - " + new Date());
            Thread.sleep(duration);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.printf("%s: The document has been"+
                    " read\n", Thread.currentThread().getName());
            readLock.unlock();
        }
    }
}
