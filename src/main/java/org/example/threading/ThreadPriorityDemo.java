package org.example.threading;
public class ThreadPriorityDemo extends Thread{
    @Override
    public void run()
    {
        System.out.println("Inside the run method");
    }

    public static void main(String[] args) {
        Thread.currentThread().setPriority(6);
        System.out.println("Main thread priority "+Thread.currentThread().getPriority());
        ThreadPriorityDemo threadPriorityDemo = new ThreadPriorityDemo();
        System.out.println("TPriorityDemo priority "+threadPriorityDemo.getPriority());
    }
}
