import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Random;
import java.util.Scanner;

public class ParallelSum {

    public static long calculatePartialSum(int[] array, int startIndex, int endIndex) {
        long sum = 0;
        for (int i = startIndex; i < endIndex; i++) {
            sum += array[i];
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter array size: ");
        int arraySize = scanner.nextInt();

        System.out.print("Enter number of parts (threads): ");
        int numberOfParts = scanner.nextInt();

        scanner.close();

        int[] array = new int[arraySize];
        Random random = new Random();
        for (int i = 0; i < arraySize; i++) {
            array[i] = random.nextInt(100);
        }

        int basePartSize = arraySize / numberOfParts;
        int remainingElements = arraySize % numberOfParts;
        int currentIndex = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(numberOfParts)) {
            List<Future<Long>> futures = new ArrayList<>();

            for (int partIndex = 0; partIndex < numberOfParts; partIndex++) {
                int startIndex = currentIndex;
                int elementsInThisPart = basePartSize + (partIndex < remainingElements ? 1 : 0);
                int endIndex = startIndex + elementsInThisPart;
                futures.add(executor.submit(() -> calculatePartialSum(array, startIndex, endIndex)));
                currentIndex = endIndex;
            }

            long totalSum = 0;
            for (Future<Long> future : futures) {
                totalSum += future.get();
            }

            System.out.println("Total sum of array elements: " + totalSum);
        }
    }
}
