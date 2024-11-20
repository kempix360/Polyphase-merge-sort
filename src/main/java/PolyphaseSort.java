import memory.BlockOfMemory;
import memory.RAM;
import memory.Record;
import memory.DiskFile;

import java.io.FileWriter;
import java.io.IOException;

class PolyphaseSort {
    private DiskFile inputTape;
    private DiskFile tape1;
    private DiskFile tape2;
    private RAM ram;
    private int phaseCount = 0;
    private int totalReadOperations = 0;
    private int totalWriteOperations = 0;
    private BlockOfMemory blockInputTape;
    private BlockOfMemory blockTape1;
    private BlockOfMemory blockTape2;
    private BlockOfMemory blockTape3;

    public PolyphaseSort(String _inputTape, String tape1File, String tape2File, RAM _ram) throws IOException {
        tape1 = new DiskFile(tape1File);
        tape2 = new DiskFile(tape2File);
        inputTape = new DiskFile(_inputTape);
        ram = _ram;
    }

    public void divideIntoTapes() throws IOException {
        // Initialize buffers for the tapes
        blockTape1 = new BlockOfMemory();
        blockTape2 = new BlockOfMemory();

        // Load the first block of input data
        blockInputTape = ram.loadToBuffer(inputTape);
        if (blockInputTape == null) {
            throw new IOException("File is empty or cannot be loaded.");
        }

        int fib1 = 1, fib2 = 1; // Fibonacci sequence initialization
        int currentFib = fib1;
        int prevArea = -1; // Initialize the previous record's area for run comparison
        int index = 0; // Index within the current block
        int lastAreaTape1 = Integer.MAX_VALUE, lastAreaTape2 = Integer.MAX_VALUE;
        DiskFile tapeToWrite = tape1;
        int joinedRuns = 0;

        while (blockInputTape != null) {
            // Process the current block until it's exhausted
            while (index < blockInputTape.getSize()) {
                // Read the next record from the current block
                Record record = ram.readRecordFromBlock(index, blockInputTape);
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
            if (index >= blockInputTape.getSize()) {
                blockInputTape = ram.loadToBuffer(inputTape);
                if (blockInputTape != null) {
                    index = 0; // Reset the index for the new block
                }
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
        blockTape1 = ram.loadToBuffer(firstTape);
        blockTape2 = ram.loadToBuffer(secondTape);
        blockInputTape = new BlockOfMemory();
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
            inputTape.runCount++;
        }

        // Write any remaining data in the output buffer to the tape
        if (blockInputTape.getSize() > 0) {
            ram.writeToFile(_tapeToWriteTo, blockInputTape, 0);
        }

        // Write the remaining data from the non-empty tape to the output tape and clear the other tape
        firstTape.resetFileOutputStream();
        secondTape.resetFileOutputStream();
        while (blockTape1 != null && tape1.runCount > 0) {
            ram.writeToFile(firstTape, blockTape1, indexTape1 - Record.RECORD_SIZE);
            blockTape1 = ram.loadToBuffer(firstTape);
            indexTape1 = 0;
        }
        while (blockTape2 != null && tape2.runCount > 0) {
            ram.writeToFile(secondTape, blockTape2, indexTape2 - Record.RECORD_SIZE);
            blockTape2 = ram.loadToBuffer(secondTape);
            indexTape2 = 0;
        }
    }

    private void writeNextRecord (DiskFile tape, Record record) {
        if (blockInputTape.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
            ram.writeToFile(tape, blockInputTape, 0);
            blockInputTape.clear();
        }
        ram.writeRecordToBlock(blockInputTape.getSize(), blockInputTape, record);
    }

    public void sort() throws IOException {
        divideIntoTapes();

        System.out.println("Data after sort:");
        Main.printFile("disk_files\\input.txt");

        System.out.println("\nTape1:");
        Main.printFile("disk_files\\tape1.txt");

        System.out.println("\nTape2:");
        Main.printFile("disk_files\\tape2.txt");

        System.out.println("\n\nNumber of runs Tape1: " + tape1.runCount);
        System.out.println("Number of runs Tape2: " + tape2.runCount);
        System.out.println("\nMerging tapes...");

        mergeTapes(
                tape1,
                tape2,
                inputTape
        );
        phaseCount = 0;

//        mergeTapes(tape1, tape2, inputTape);
//        phaseCount++;
//        while (tape1.runCount + tape2.runCount + inputTape.runCount != 1) {
//            mergeTapes(tape1, tape2, inputTape);
//            phaseCount++;
//            // Swap the tapes for the next phase
//            DiskFile temp;
//            if (tape1.runCount > tape2.runCount) {
//                temp = tape2;
//                tape2 = inputTape;
//            }
//            else {
//                temp = tape1;
//                tape1 = inputTape;
//            }
//            inputTape = temp;
//        }

    }


    public void printStats() {
        System.out.println("Number of sort phases: " + phaseCount);
        System.out.println("Number of read operations: " + totalReadOperations);
        System.out.println("Number of write operations: " + totalWriteOperations);
    }


    // TODO:
    // 1. dividing input into 2 tapes
    // 2. merging the contents of the tapes
    // 3. saving to the initial file and counting number of phases
    // ogarnac jak dziala sklejanie z dummy runami
    // distribution z wykrywaniem sklejania

}
