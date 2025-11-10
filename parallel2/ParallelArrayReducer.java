import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelArrayReducer {

    public static long parallelSum(long[] array, ExecutorService threadPool) {
        int activeLength = array.length;
        while (activeLength > 1) {
            int numTasks = activeLength / 2;
            CountDownLatch waveLatch = new CountDownLatch(numTasks);
            final int currentActiveLength = activeLength;
            for (int i = 0; i < numTasks; i++) {
                final int taskIndex = i;
                threadPool.submit(() -> {
                    try {
                        int leftIndex = taskIndex;
                        int rightIndex = currentActiveLength - 1 - taskIndex;
                        array[leftIndex] = array[leftIndex] + array[rightIndex];
                    } finally {
                        waveLatch.countDown();
                    }
                });
            }
            try {
                waveLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Керуючий потік було перервано під час очікування хвилі.");
                return -1L;
            }
            activeLength = (activeLength + 1) / 2;
        }
        return array[0];
    }

    public static long sequentialSum(long[] array) {
        int activeLength = array.length;
        while (activeLength > 1) {
            int numTasks = activeLength / 2;
            final int currentActiveLength = activeLength;
            for (int i = 0; i < numTasks; i++) {
                final int taskIndex = i;
                int leftIndex = taskIndex;
                int rightIndex = currentActiveLength - 1 - taskIndex;
                array[leftIndex] = array[leftIndex] + array[rightIndex];
            }
            activeLength = (activeLength + 1) / 2;
        }
        return array[0];
    }

    public static void main(String[] args) {
        final int ARRAY_LENGTH = 20000000;
        final int MAX_RANDOM_VALUE = 10;

        long[] array = new long[ARRAY_LENGTH];
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = rand.nextInt(MAX_RANDOM_VALUE);
        }
        System.out.println("Розмір масиву: " + ARRAY_LENGTH);

        // long sequentialSum = 0;
        // long startTimeSequential = System.nanoTime();
        // sequentialSum = ParallelArrayReducer.sequentialSum(array);
        // long endTimeSequential = System.nanoTime();
        // double durationMsSequential = (endTimeSequential - startTimeSequential) /
        // 1_000_000.0;
        // System.out.println("\n--- Послідовне виконання ---");
        // System.out.println("Контрольна сума: " + sequentialSum);
        // System.out.printf("Час виконання: %.4f мс%n", durationMsSequential);

        int numCores = Runtime.getRuntime().availableProcessors();
        System.out.println("\n--- Паралельне виконання (" + numCores + " потоків) ---");
        ExecutorService threadPool = Executors.newFixedThreadPool(numCores);
        long startTimeParallel = System.nanoTime();
        long parallelSum = ParallelArrayReducer.parallelSum(array, threadPool);
        long endTimeParallel = System.nanoTime();
        double durationMsParallel = (endTimeParallel - startTimeParallel) / 1_000_000.0;
        System.out.println("Паралельна сума: " + parallelSum);
        System.out.printf("Час виконання: %.4f мс%n", durationMsParallel);
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // System.out.println("\nВерифікація: " + (sequentialSum == parallelSum ?
        // "УСПІХ" : "ПОМИЛКА"));
    }
}