#include <mpi.h>
#include <iostream>
#include <vector>
#include <cstdlib>
#include <ctime>

#define TAG_WORK 1
#define TAG_STOP 0

int main(int argc, char **argv)
{
  MPI_Init(&argc, &argv);

  int world_rank;
  int world_size;

  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);

  if (world_rank == 0)
  {
    int arraySize = 100'000;
    std::vector<int> array(arraySize);

    std::srand(std::time(nullptr));
    for (int i = 0; i < arraySize; i++)
    {
      array[i] = 1;
    }

    double start_time = MPI_Wtime();

    int activeLength = arraySize;

    while (activeLength > 1)
    {
      int numPairs = activeLength / 2;
      int pairsSent = 0;
      int pairsReceived = 0;

      int numWorkers = world_size - 1;

      int resultBuf[2];
      MPI_Status status;

      for (int w = 1; w <= numWorkers && pairsSent < numPairs; w++)
      {
        int leftIdx = pairsSent;
        int rightIdx = activeLength - 1 - leftIdx;

        int taskBuf[3] = {array[leftIdx], array[rightIdx], leftIdx};

        MPI_Send(taskBuf, 3, MPI_INT, w, TAG_WORK, MPI_COMM_WORLD);
        pairsSent++;
      }

      while (pairsReceived < numPairs)
      {
        MPI_Recv(resultBuf, 2, MPI_INT, MPI_ANY_SOURCE, TAG_WORK, MPI_COMM_WORLD, &status);

        int sum = resultBuf[0];
        int idx = resultBuf[1];
        array[idx] = sum;

        pairsReceived++;
        int workerRank = status.MPI_SOURCE;

        if (pairsSent < numPairs)
        {
          int leftIdx = pairsSent;
          int rightIdx = activeLength - 1 - leftIdx;

          int taskBuf[3] = {array[leftIdx], array[rightIdx], leftIdx};
          MPI_Send(taskBuf, 3, MPI_INT, workerRank, TAG_WORK, MPI_COMM_WORLD);
          pairsSent++;
        }
      }

      activeLength = (activeLength + 1) / 2;
    }

    for (int w = 1; w < world_size; w++)
    {
      MPI_Send(NULL, 0, MPI_INT, w, TAG_STOP, MPI_COMM_WORLD);
    }

    double end_time = MPI_Wtime();

    std::cout << "Total sum: " << array[0] << std::endl;
    std::cout << "Execution time: " << (end_time - start_time) << " seconds." << std::endl;
  }
  else
  {
    while (true)
    {
      int taskBuf[3]; // {val1, val2, index}
      MPI_Status status;

      MPI_Recv(taskBuf, 3, MPI_INT, 0, MPI_ANY_TAG, MPI_COMM_WORLD, &status);

      if (status.MPI_TAG == TAG_STOP)
      {
        break;
      }

      int val1 = taskBuf[0];
      int val2 = taskBuf[1];
      int index = taskBuf[2];

      int sum = val1 + val2;

      int resultBuf[2] = {sum, index};
      MPI_Send(resultBuf, 2, MPI_INT, 0, TAG_WORK, MPI_COMM_WORLD);
    }
  }

  MPI_Finalize();
  return 0;
}