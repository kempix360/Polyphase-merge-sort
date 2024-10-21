import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class PolyphaseSort {
    private Tape tape1;
    private Tape tape2;
    private Tape tape3;
    private int phaseCount = 0;
    private int totalReadOperations = 0;
    private int totalWriteOperations = 0;

    public PolyphaseSort(String inputFile, String tape1File, String tape2File) throws IOException {
        tape1 = new Tape(tape1File);
        tape2 = new Tape(tape2File);
        tape3 = new Tape(inputFile);
    }

    public void divideIntoTapes() throws IOException {
        int[] record = tape3.readRecord();
        totalReadOperations++;
        int prev_area = -1;
        Tape tapeToAdd = tape1;
        while (record != null) {
            int totalArea = 2 * (record[0] * record[1] + record[0] * record[2] + record[1] * record[2]);
            if (totalArea < prev_area) {
                if (tapeToAdd == tape1)
                    tapeToAdd = tape2;
                else
                    tapeToAdd = tape1;
            }
            tapeToAdd.writeRecord(record);
            totalWriteOperations++;
            record = tape3.readRecord();
            totalReadOperations++;
        }
    }

    public void sort() throws IOException {
        // List to store sequences (each record is an array of integers)
        List<int[]> sequence = new ArrayList<>();
        int[] record = tape3.readRecord();

        while (record != null) {
            sequence.add(record); // Add the current record (array) to the sequence list
            record = tape3.readRecord();
        }

        sequence.sort((a, b) -> {
            int areaA = 2 * (a[0] * a[1] + a[1] * a[2] + a[0] * a[2]);
            int areaB = 2 * (b[0] * b[1] + b[1] * b[2] + b[0] * b[2]);
            return Integer.compare(areaA, areaB);
        });

        for (int[] numbers : sequence) {
            tape1.writeRecord(numbers);
        }

        phaseCount++;
        updateStats();
    }


    private void updateStats() {
        totalReadOperations += tape3.getReadOperations() + tape1.getReadOperations() + tape2.getReadOperations();
        totalWriteOperations += tape1.getWriteOperations() + tape2.getWriteOperations();
    }

    public void printStats() {
        System.out.println("Number of sort phases: " + phaseCount);
        System.out.println("Number of read operations: " + totalReadOperations);
        System.out.println("Number of write operations: " + totalWriteOperations);
    }

    public void close() throws IOException {
        tape3.close();
        tape1.close();
        tape2.close();
    }

    // TODO:
    // 1. dividing input into 2 tapes
    // 2. merging the contents of the tapes
    // 3. saving to the initial file and counting number of phases
}
