package org.example;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongBenchmark {
    private static final int ITERATIONS = 100_000_000;

    public static void main(String[] args) {
        // Benchmark AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            atomicLong.incrementAndGet();
        }
        long endTime = System.nanoTime();
        System.out.println("AtomicLong time: " + (endTime - startTime) / 1_000_000 + " ms");

        // Benchmark regular long
        long regularLong = 0;
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            regularLong++;
        }
        endTime = System.nanoTime();
        System.out.println("Regular long time: " + (endTime - startTime) / 1_000_000 + " ms");
    }
}