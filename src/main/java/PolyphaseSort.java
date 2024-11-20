import memory.BlockOfMemory;
import memory.RAM;
import memory.Record;
import memory.DiskFile;

import java.io.*;

class PolyphaseSort {
    private String fileToSortPath;
    private DiskFile tape1;
    private DiskFile tape2;
    private DiskFile tape3;
    private final RAM ram;
    private int phaseCount = 0;
    private BlockOfMemory blockTape3;
    private BlockOfMemory blockTape1;
    private BlockOfMemory blockTape2;

    public PolyphaseSort(String _inputTape, String tape1File, String tape2File, RAM _ram) throws IOException {
        fileToSortPath = _inputTape;
        tape1 = new DiskFile(tape1File);
        tape2 = new DiskFile(tape2File);
        tape3 = new DiskFile(_inputTape);
        ram = _ram;
    }

    public void sort() throws IOException {
        // ANSI color codes
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String GREEN = "\u001B[32m";
        final String RED = "\u001B[31m";

        System.out.println("\n---------------------------------");
        System.out.println(YELLOW + "Data before sort:" + RESET);
        printFile(fileToSortPath);

        divideIntoTapes();

        while (tape1.runCount + tape2.runCount + tape3.runCount != 1) {
            mergeTapes(tape1, tape2, tape3);
            // Swap the tapes for the next phase
            DiskFile temp;
            if (tape1.runCount > tape2.runCount) {
                temp = tape2;
                tape2 = tape3;
            }
            else {
                temp = tape1;
                tape1 = tape3;
            }
            tape3 = temp;
            phaseCount++;
        }

        DiskFile finalTape = (tape1.runCount == 1) ? tape1 : tape2;
        writeFinalTapeToInput(finalTape, fileToSortPath);

        System.out.println("\n---------------------------------");
        System.out.println(YELLOW + "Data after sort:" + RESET);
        printFile(fileToSortPath);
        System.out.println("\n---------------------------------");
        System.out.println(CYAN + "Sorting statistics:" + RESET);
        printStats();
    }

    public void divideIntoTapes() throws IOException {
        // Initialize buffers for the tapes
        blockTape1 = new BlockOfMemory();
        blockTape2 = new BlockOfMemory();

        // Load the first block of input data
        blockTape3 = ram.loadToBuffer(tape3);
        if (blockTape3 == null) {
            throw new IOException("File is empty or cannot be loaded.");
        }

        int fib1 = 1, fib2 = 1; // Fibonacci sequence initialization
        int currentFib = fib1;
        int prevArea = -1; // Initialize the previous record's area for run comparison
        int index = 0; // Index within the current block
        int lastAreaTape1 = Integer.MAX_VALUE, lastAreaTape2 = Integer.MAX_VALUE;
        DiskFile tapeToWrite = tape1;
        int joinedRuns = 0;

        while (blockTape3 != null) {
            // Process the current block until it's exhausted
            while (index < blockTape3.getSize()) {
                // Read the next record from the current block
                Record record = ram.readRecordFromBlock(index, blockTape3);
                index += Record.RECORD_SIZE;
                int totalArea = record.getArea();

                // Check for a new run
                if (totalArea < prevArea) {
                    tapeToWrite.runCount++;
                    if (tapeToWrite.runCount == currentFib) {
                        if (tapeToWrite == tape1) { // update latest area for the tape
                            lastAreaTape1 = prevArea;
                        } else {
                            lastAreaTape2 = prevArea;
                        }
                        // Switch to the other tape
                        tapeToWrite = (tapeToWrite == tape1) ? tape2 : tape1;

                        // check if there will be no joined runs (if yes, decrement run count)
                        if (tapeToWrite == tape1 && record.getArea() > lastAreaTape1) {
                            tapeToWrite.runCount--;
                            joinedRuns++;
                        } else if (tapeToWrite == tape2 && record.getArea() > lastAreaTape2) {
                            tapeToWrite.runCount--;
                            joinedRuns++;
                        }

                        // Update Fibonacci sequence
                        currentFib = fib1 + fib2;
                        fib2 = fib1;
                        fib1 = currentFib;
                    }
                }

                // Write the record to the appropriate tape's buffer
                BlockOfMemory blockToWrite = (tapeToWrite == tape1) ? blockTape1 : blockTape2;
                if (blockToWrite.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
                    // Buffer is full: write it to the tape and clear it
                    ram.writeToFile(tapeToWrite, blockToWrite, 0);
                    blockToWrite.clear();
                }

                ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record);
                prevArea = totalArea; // Update the previous area
            }

            // Load the next block of input data if the current block is fully processed
            if (index >= blockTape3.getSize()) {
                blockTape3 = ram.loadToBuffer(tape3);
                index = 0;
            }
        }

        // Write any remaining data in buffers to the tapes
        if (blockTape1.getSize() > 0) {
            ram.writeToFile(tape1, blockTape1, 0);
        }
        if (blockTape2.getSize() > 0) {
            ram.writeToFile(tape2, blockTape2, 0);
        }

        // Ensure Fibonacci sequence consistency
        while (tapeToWrite.getRunCount() < fib1) {
            tapeToWrite.runCount++;
        }
    }


    public void mergeTapes(DiskFile firstTape, DiskFile secondTape, DiskFile _tapeToWriteTo) throws IOException {
        // Buffers for reading from tapes and writing to the output tape
        firstTape.resetFileInputStream();
        secondTape.resetFileInputStream();
        firstTape.resetScanner();
        secondTape.resetScanner();

        blockTape1 = ram.loadToBuffer(firstTape);
        blockTape2 = ram.loadToBuffer(secondTape);
        blockTape3 = new BlockOfMemory();
        _tapeToWriteTo.resetFileOutputStream();

        int indexTape1 = 0, indexTape2 = 0;
        int prevAreaTape1 = -1, prevAreaTape2 = -1;
        int numOfRuns = Math.min(firstTape.runCount, secondTape.runCount);

        Record record1 = null, record2 = null;
        // Fetch the first records from the tapes
        record1 = (blockTape1 != null) ? ram.readRecordFromBlock(indexTape1, blockTape1) : null;
        indexTape1 += Record.RECORD_SIZE;
        record2 = (blockTape2 != null) ? ram.readRecordFromBlock(indexTape2, blockTape2) : null;
        indexTape2 += Record.RECORD_SIZE;


        boolean isEndRun1, isEndRun2;
        // Continue merging until reached number of runs
        for (int i = 0; i < numOfRuns; i++) {
            int runCount = 0;
            isEndRun1 = isEndRun2 = false;

            while (!isEndRun1 || !isEndRun2) {
                if (record1 == null && record2 == null) {
                    break;
                }
                if (record1 == null) {
                    isEndRun1 = true;
                }
                if (record2 == null) {
                    isEndRun2 = true;
                }
                if (isEndRun2 || (!isEndRun1 && record1.getArea() <= record2.getArea())) {
                    writeNextRecord(_tapeToWriteTo, record1);
                    prevAreaTape1 = record1.getArea();

                    // read next record from tape1 and reload block if needed
                    if (blockTape1 != null && indexTape1 >= blockTape1.getSize()) {
                        blockTape1 = ram.loadToBuffer(firstTape);
                        indexTape1 = 0;
                    }
                    record1 = (blockTape1 != null) ? ram.readRecordFromBlock(indexTape1, blockTape1) : null;
                    indexTape1 += Record.RECORD_SIZE;

                    if (record1 == null || record1.getArea() < prevAreaTape1) {
                        isEndRun1 = true;
                    }
                } else {
                    writeNextRecord(_tapeToWriteTo, record2);
                    prevAreaTape2 = record2.getArea();

                    // read next record from tape2 and reload block if needed
                    if (blockTape2 != null && indexTape2 >= blockTape2.getSize()) {
                        blockTape2 = ram.loadToBuffer(secondTape);
                        indexTape2 = 0;
                    }
                    record2 = (blockTape2 != null) ? ram.readRecordFromBlock(indexTape2, blockTape2) : null;
                    indexTape2 += Record.RECORD_SIZE;

                    if (record2 == null || record2.getArea() < prevAreaTape2) {
                        isEndRun2 = true;
                    }
                }
            }
            firstTape.runCount--;
            secondTape.runCount--;
            tape3.runCount++;
        }

        // Write any remaining data in the output buffer to the tape
        if (blockTape3.getSize() > 0) {
            ram.writeToFile(_tapeToWriteTo, blockTape3, 0);
        }

        // Write the remaining data from the non-empty tape to the output tape and clear the other tape
        firstTape.resetFileOutputStream();
        secondTape.resetFileOutputStream();
        while (blockTape1 != null && tape1.runCount > 0) {
            ram.writeToFile(firstTape, blockTape1, indexTape1);
            blockTape1 = ram.loadToBuffer(firstTape);
            indexTape1 = 0;
        }
        while (blockTape2 != null && tape2.runCount > 0) {
            ram.writeToFile(secondTape, blockTape2, indexTape2);
            blockTape2 = ram.loadToBuffer(secondTape);
            indexTape2 = 0;
        }
    }

    private void writeNextRecord (DiskFile tape, Record record) {
        if (blockTape3.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
            ram.writeToFile(tape, blockTape3, 0);
            blockTape3.clear();
        }
        ram.writeRecordToBlock(blockTape3.getSize(), blockTape3, record);
    }

    private void writeFinalTapeToInput(DiskFile finalTape, String inputFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFilePath))) {
            finalTape.resetFileInputStream();
            finalTape.resetScanner();
            BlockOfMemory block = ram.loadToBuffer(finalTape);
            int index = 0;

            while (block != null) {
                while (index < block.getSize()) {
                    Record record = ram.readRecordFromBlock(index, block);
                    if (record == null) {
                        break;
                    }
                    writer.write(record.toString());
                    writer.newLine();
                    index += Record.RECORD_SIZE;
                }
                block = ram.loadToBuffer(finalTape);
                index = 0;
            }
        }
    }

    public void printStats() {
        System.out.println("Number of sort phases: " + phaseCount);
        System.out.println("Number of read operations: " + ram.getTotalReadOperations());
        System.out.println("Number of write operations: " + ram.getTotalWriteOperations());
    }

    public static void printFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int prevArea = -1;
//            System.out.println("New run");
//            int runCount = 1;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+"); // Split by whitespace
                int length = Integer.parseInt(values[0]);
                int width = Integer.parseInt(values[1]);
                int height = Integer.parseInt(values[2]);
                Record record = new Record(length, width, height);
//                if (record.getArea() < prevArea) {
//                    System.out.println("New run");
//                    runCount++;
//                }
//                prevArea = record.getArea();
                System.out.println(record);
            }
        }
    }

}
