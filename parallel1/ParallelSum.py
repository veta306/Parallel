import random
import time
from concurrent.futures import ProcessPoolExecutor

def calculate_partial_sum(numbers, start_index, end_index):
    return sum(numbers[start_index:end_index])

def main():
    array_size = int(input("Enter array size: "))
    number_of_parts = int(input("Enter number of parts (processes): "))

    numbers = [random.randint(0, 99) for _ in range(array_size)]

    base_part_size = array_size // number_of_parts
    remaining_elements = array_size % number_of_parts

    indices = []
    current_index = 0
    for part_index in range(number_of_parts):
        start_index = current_index
        elements_in_part = base_part_size + (1 if part_index < remaining_elements else 0)
        end_index = start_index + elements_in_part
        indices.append((start_index, end_index))
        current_index = end_index

    with ProcessPoolExecutor(max_workers=number_of_parts) as executor:
        futures = [
            executor.submit(calculate_partial_sum, numbers, start, end)
            for start, end in indices
        ]
        total_sum = sum(f.result() for f in futures)

    print(f"Total sum of array elements: {total_sum}")

if __name__ == "__main__":
    main()
