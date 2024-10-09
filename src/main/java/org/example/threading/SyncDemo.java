package org.example.threading;
/*
Method 1: Synchronizing a block
Method 2 : synchronizing a method
 */
class Sender{
    //public synchronized void send(String msg) -->Method 2
    public void send(String msg)
    {
        System.out.println("Sending\t" + msg);
        try {
            Thread.sleep(1000);
        }
        catch (Exception e) {
            System.out.println("Thread  interrupted.");
        }
        System.out.println("\n" + msg + "Sent");
    }
}
class ThreadedSend extends Thread {
    private final String msg;
    private final Sender sender;

    ThreadedSend(String msg, Sender sender) {
        this.msg = msg;
        this.sender = sender;
    }

    public void run()
    {
        //Method 1:
        synchronized (sender)
        {
            sender.send(msg);
        }
    }
}
public class SyncDemo {
    public static void main(String[] args) {
        Sender sender = new Sender();
        ThreadedSend hiSend = new ThreadedSend("Hi",sender);
        //To create a daemon thread
//        hiSend.setDaemon(true);
        ThreadedSend byeSend = new ThreadedSend("Bye",sender);
//        Runnable task = () ->{
//            System.out.println("Hello Virtual Thread!!");
//        };
//        Thread.startVirtualThread(task);
        hiSend.start();
        byeSend.start();
        try{
            hiSend.join();
            byeSend.join();
        } catch (InterruptedException e) {
            System.out.println("Thread got interrupted");
        }
    }
}
