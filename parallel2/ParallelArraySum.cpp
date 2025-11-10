#include <iostream>
#include <vector>
#include <string>
#include <random>
#include <numeric>
#include <chrono>
#include <omp.h>
#include <windows.h>

long long calculateSumOpenMP(std::vector<long long> &arr)
{
    if (arr.empty())
    {
        return 0;
    }

    int activeLength = arr.size();

    while (activeLength > 1)
    {

        int numTasks = activeLength / 2;

#pragma omp parallel for
        for (int i = 0; i < numTasks; i++)
        {
            int rightIndex = activeLength - 1 - i;
            arr[i] = arr[i] + arr[rightIndex];
        }

        activeLength = (activeLength + 1) / 2;
    }

    return arr[0];
}

long long calculateSumSequential(std::vector<long long> &arr)
{
    if (arr.empty())
    {
        return 0;
    }

    int activeLength = arr.size();

    while (activeLength > 1)
    {
        int numTasks = activeLength / 2;

        for (int i = 0; i < numTasks; i++)
        {
            int rightIndex = activeLength - 1 - i;
            arr[i] = arr[i] + arr[rightIndex];
        }

        activeLength = (activeLength + 1) / 2;
    }

    return arr[0];
}

int main()
{
    SetConsoleOutputCP(65001);

    const int ARRAY_LENGTH = 20000000;
    const int MAX_RANDOM_VALUE = 10;

    std::vector<long long> arr(ARRAY_LENGTH);

    std::mt19937 gen(std::random_device{}());
    std::uniform_int_distribution<> dis(0, MAX_RANDOM_VALUE);

    for (int i = 0; i < ARRAY_LENGTH; ++i)
    {
        arr[i] = dis(gen);
    }

    std::cout << "Розмір масиву: " << ARRAY_LENGTH << std::endl;

    // long long sequentialSum = 0;

    // auto startTimeSequential = std::chrono::high_resolution_clock::now();

    // sequentialSum = calculateSumSequential(arr);

    // auto endTimeSequential = std::chrono::high_resolution_clock::now();
    // std::chrono::duration<double, std::milli> durationMsSequential = endTimeSequential - startTimeSequential;

    // std::cout << "\n--- Послідовне виконання ---" << std::endl;
    // std::cout << "Контрольна сума: " << sequentialSum << std::endl;
    // std::cout.precision(4);
    // std::cout << "Час виконання: " << std::fixed << durationMsSequential.count() << " мс" << std::endl;

    int numThreads = omp_get_max_threads();
    omp_set_num_threads(numThreads);
    std::cout << "\n--- Паралельне виконання (OpenMP, " << numThreads << " потоків) ---" << std::endl;

    auto startTimeParallel = std::chrono::high_resolution_clock::now();

    long long parallelSum = calculateSumOpenMP(arr);

    auto endTimeParallel = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> durationMsParallel = endTimeParallel - startTimeParallel;

    std::cout << "Паралельна сума: " << parallelSum << std::endl;
    std::cout << "Час виконання: " << std::fixed << durationMsParallel.count() << " мс" << std::endl;

    // std::cout << "\nВерифікація: " << (sequentialSum == parallelSum ? "УСПІХ" : "ПОМИЛКА") << std::endl;

    return 0;
}