package org.example.threading;
class ThreadTest implements Runnable
{
    public void run()
    {
        try{
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("State of thread1 while it called join() method on thread2 -"
                +Test.thread1.getState());
        try{
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
public class Test implements Runnable {
    public static Thread thread1;
    public static Test obj;

    public static void main(String[] args) {
        obj = new Test();
        thread1 = new Thread(obj);
        System.out.println("State of thread1 after creating it - "
                        + thread1.getState());
        thread1.start();
        System.out.println("Current thread is "+Thread.currentThread().getName());
        System.out.println("State of thread1 after calling .start() method on it - "
                        + thread1.getState());
    }
    @Override
    public void run() {
        ThreadTest myThread = new ThreadTest();
        Thread thread2 = new Thread(myThread);
        thread2.setPriority(8);
        System.out.println("State of thread2 after creating it - "
                        + thread2.getState());
        thread2.start();
        System.out.println("State of thread2 after calling .start() method on it - "
                        + thread2.getState());
        try{
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("State of thread2 after calling .sleep() method on it - "
                        + thread2.getState());
        try {
            thread2.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("State of thread2 when it has finished it's execution - "
                        + thread2.getState());
    }
}
