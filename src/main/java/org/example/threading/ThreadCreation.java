package org.example.threading;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * <h2>Different ways to create a thread</h2>
 * <ol>
 *     <li>Extend the Thread class</li>
 *     <li>Implement Runnable interface</li>
 *     <li>Using Lambda Expression</li>
 *     <li>Using Executor Service Framework</li>
 *     <li>Using Timer Task</li>
 *     <li>Using Fork Join pool</li>
 *     <li>Using Callable and Future</li>
 *     <li>Using Thread Factory</li>
 * </ol>
 */
//METHOD 1: EXTEND THREAD CLASS
class MyThread extends Thread
{
    @Override
    public void run()
    {
        System.out.println("Thread is running using Thread class");
    }
}
//METHOD 2: USING RUNNABLE INTERFACE
class MyRunnable implements Runnable
{
    public void run()
    {
        System.out.println("Thread is running using Runnable interface");
    }
}
//METHOD 6: USING FORK JOIN POOL
class MyRecursiveTask extends RecursiveTask<Integer>
{
    private int workload;

    public MyRecursiveTask(int workload) {
        this.workload = workload;
    }

    @Override
    protected Integer compute() {
        if(workload>1)
        {
            MyRecursiveTask task1 = new MyRecursiveTask(workload/2);
            MyRecursiveTask task2 = new MyRecursiveTask(workload/2);
            task1.fork();
            task2.fork();
            return task1.join()+task2.join();
        }
        else
        {
            return workload;
        }
    }
}
class CustomThreadFactory implements ThreadFactory{
    private int count = 0;
    @Override
    public Thread newThread(Runnable r) {
        count++;
        Thread thread = new Thread(r);
        thread.setName("Custom Thread- "+count);
        return thread;
    }
}

public class ThreadCreation {
    public static void main(String[] args) throws Exception {
        //METHOD 1: EXTEND THREAD CLASS
        MyThread myThread = new MyThread();
        myThread.start();
        System.out.println("1-------------------------------------------------------");

        //METHOD 2: IMPLEMENT RUNNABLE INTERFACE
        MyRunnable myRunnable = new MyRunnable();
        Thread thread1 = new Thread(myRunnable);
        thread1.start();
        System.out.println("2-------------------------------------------------------");

        //METHOD 3: USING LAMBDA EXPRESSION
        Thread thread2 = new Thread(()->{
            System.out.println("Thread is created using lambda expression");
        });
        thread2.start();
        System.out.println("3-------------------------------------------------------");

        //METHOD 4: USING EXECUTOR SERVICE FRAMEWORK
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(()->{
            System.out.println("Thread is running through Executor Service");
        });
        executorService.shutdown();
        System.out.println("4-------------------------------------------------------");

        //METHOD 6: USING FORK JOIN POOL
        ForkJoinPool pool = new ForkJoinPool();
        MyRecursiveTask myRecursiveTask = new MyRecursiveTask(128);
        int result = pool.invoke(myRecursiveTask);
        System.out.println("Result is "+result);
        System.out.println("6-------------------------------------------------------");

        //METHOD 7: USING CALLABLE AND FUTURE
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> callable = () ->{
            return "Callable thread executed";
        };
        Future<String> future = executor.submit(callable);
        System.out.println(future.get());
        executor.shutdown();
        System.out.println("7-------------------------------------------------------");


        //METHOD 8: USING THREAD FACTORY
        ThreadFactory factory = new CustomThreadFactory();
        ExecutorService executorService1 = Executors.newFixedThreadPool(2,factory);
        executorService1.submit(()->{
            System.out.println("Thread running using Thread Factory");
        });
        executorService1.shutdown();
        System.out.println("8-------------------------------------------------------");


        //METHOD 5: USING TIMER AND TIMER TASK
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Thread running using Timer Task");
            }
        },1000);
    }
}
