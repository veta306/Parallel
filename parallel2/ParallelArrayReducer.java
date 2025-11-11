import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
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

    private static class WaveTask extends RecursiveAction {
        private final long[] array;
        private final int low;
        private final int high;
        private final int currentActiveLength;

        WaveTask(long[] array, int low, int high, int currentActiveLength) {
            this.array = array;
            this.low = low;
            this.high = high;
            this.currentActiveLength = currentActiveLength;
        }

        @Override
        protected void compute() {
            int length = high - low;
            if (length <= 1) {
                int leftIndex = low;
                int rightIndex = currentActiveLength - 1 - leftIndex;
                array[leftIndex] = array[leftIndex] + array[rightIndex];
            } else {
                int mid = (low + high) / 2;
                WaveTask leftTask = new WaveTask(array, low, mid, currentActiveLength);
                WaveTask rightTask = new WaveTask(array, mid, high, currentActiveLength);
                invokeAll(leftTask, rightTask);
            }
        }
    }

    public static long forkJoinSum(long[] array, ForkJoinPool forkJoinPool) {
        int activeLength = array.length;
        while (activeLength > 1) {
            int numTasks = activeLength / 2;
            final int currentActiveLength = activeLength;
            WaveTask topTask = new WaveTask(array, 0, numTasks, currentActiveLength);
            forkJoinPool.invoke(topTask);
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

    public static void shutdownPool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        final int ARRAY_LENGTH = 11;
        final int MAX_RANDOM_VALUE = 10;

        long[] array = new long[ARRAY_LENGTH];
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = rand.nextInt(MAX_RANDOM_VALUE);
        }
        System.out.println("Розмір масиву: " + ARRAY_LENGTH);

        long[] arrayForParallel = Arrays.copyOf(array, array.length);
        long[] arrayForForkJoin = Arrays.copyOf(array, array.length);

        long sequentialSum = 0;
        long startTimeSequential = System.nanoTime();
        sequentialSum = sequentialSum(array);
        long endTimeSequential = System.nanoTime();
        double durationMsSequential = (endTimeSequential - startTimeSequential) /
                1_000_000.0;
        System.out.println("\n--- Послідовне виконання ---");
        System.out.println("Контрольна сума: " + sequentialSum);
        System.out.printf("Час виконання: %.4f мс%n", durationMsSequential);

        int numCores = Runtime.getRuntime().availableProcessors();

        ForkJoinPool forkJoinPool = new ForkJoinPool(numCores);
        System.out.println("\n--- Паралельне (ForkJoinPool, " + numCores + " потоків) ---");
        long startTimeForkJoin = System.nanoTime();
        long forkJoinSumResult = forkJoinSum(arrayForForkJoin, forkJoinPool);
        long endTimeForkJoin = System.nanoTime();
        double durationMsForkJoin = (endTimeForkJoin - startTimeForkJoin) / 1_000_000.0;
        System.out.println("ForkJoin сума: " + forkJoinSumResult);
        System.out.printf("Час виконання: %.4f мс%n", durationMsForkJoin);
        shutdownPool(forkJoinPool);

        ExecutorService threadPool = Executors.newFixedThreadPool(numCores);
        System.out.println("\n--- Паралельне виконання (" + numCores + " потоків) ---");
        long startTimeParallel = System.nanoTime();
        long parallelSum = parallelSum(arrayForParallel, threadPool);
        long endTimeParallel = System.nanoTime();
        double durationMsParallel = (endTimeParallel - startTimeParallel) / 1_000_000.0;
        System.out.println("Паралельна сума: " + parallelSum);
        System.out.printf("Час виконання: %.4f мс%n", durationMsParallel);
        shutdownPool(threadPool);
    }
}