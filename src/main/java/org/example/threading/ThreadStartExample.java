package org.example.threading;
class TestThread extends Thread
{
    public void run()
    {
        try
        {
            System.out.println ("Thread " + Thread.currentThread().getId() + " is running");

        }
        catch (Exception e)
        {
            System.out.println ("Exception is caught");
        }
    }
}
public class ThreadStartExample {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            TestThread thread = new TestThread();
            thread.start();
        }
    }
}
