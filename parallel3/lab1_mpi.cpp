#include <mpi.h>
#include <iostream>
#include <vector>
#include <cstdlib>
#include <ctime>

long long calculatePartialSum(const std::vector<char> &local_array)
{
  long long sum = 0;
  for (int num : local_array)
  {
    sum += num;
  }
  return sum;
}

int main(int argc, char **argv)
{
  MPI_Init(&argc, &argv);

  int world_rank;
  int world_size;

  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);

  int arraySize = 1'000'000'000;
  std::vector<char> global_array;

  if (world_rank == 0)
  {
    global_array.resize(arraySize);
    std::srand(std::time(nullptr));
    for (int i = 0; i < arraySize; i++)
    {
      global_array[i] = std::rand() % 10;
    }
  }

  MPI_Bcast(&arraySize, 1, MPI_INT, 0, MPI_COMM_WORLD);

  std::vector<int> send_counts(world_size);
  std::vector<int> displacements(world_size);

  int basePartSize = arraySize / world_size;
  int remainingElements = arraySize % world_size;
  int current_idx = 0;

  for (int i = 0; i < world_size; i++)
  {
    send_counts[i] = basePartSize + (i < remainingElements ? 1 : 0);
    displacements[i] = current_idx;
    current_idx += send_counts[i];
  }

  std::vector<char> local_array(send_counts[world_rank]);

  MPI_Barrier(MPI_COMM_WORLD);
  double start_time = MPI_Wtime();

  MPI_Scatterv(global_array.data(), send_counts.data(), displacements.data(), MPI_CHAR,
               local_array.data(), send_counts[world_rank], MPI_CHAR,
               0, MPI_COMM_WORLD);

  long long local_sum = calculatePartialSum(local_array);

  long long total_sum = 0;

  MPI_Reduce(&local_sum, &total_sum, 1, MPI_LONG_LONG, MPI_SUM, 0, MPI_COMM_WORLD);

  double end_time = MPI_Wtime();

  if (world_rank == 0)
  {
    std::cout << "Total sum of array elements: " << total_sum << std::endl;
    std::cout << "Execution time: " << (end_time - start_time) << " seconds." << std::endl;
  }

  MPI_Finalize();
  return 0;
}