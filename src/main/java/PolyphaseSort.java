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

    public PolyphaseSort(String _inputTape, String tape1File, String tape2File, RAM _ram) throws IOException {
        tape1 = new DiskFile(tape1File);
        tape2 = new DiskFile(tape2File);
        inputTape = new DiskFile(_inputTape);
        ram = _ram;
    }

    public void divideIntoTapes(DiskFile firstTape, DiskFile secondTape) throws IOException {
        // Initialize buffers for the tapes
        BlockOfMemory blockFirstTape = new BlockOfMemory();
        BlockOfMemory blockSecondTape = new BlockOfMemory();

        // Load the first block of input data
        BlockOfMemory blockOfInput = ram.loadToBuffer(inputTape);
        if (blockOfInput == null) {
            throw new IOException("File is empty or cannot be loaded.");
        }

        int fib1 = 1, fib2 = 1; // Fibonacci sequence initialization
        int currentFib = fib1;
        int prevArea = -1; // Initialize the previous record's area for run comparison
        int index = 0; // Index within the current block
        int lastAreaTape1 = Integer.MAX_VALUE, lastAreaTape2 = Integer.MAX_VALUE;
        DiskFile tapeToWrite = firstTape;

        while (blockOfInput != null) {
            // Process the current block until it's exhausted
            while (index < blockOfInput.getSize()) {
                // Read the next record from the current block
                Record record = ram.readRecordFromBlock(index, blockOfInput);
                index += Record.RECORD_SIZE;
                int totalArea = record.getArea();

                // Check for a new run
                if (totalArea < prevArea) {
                    tapeToWrite.runCount++;
                    if (tapeToWrite.runCount == currentFib) {
                        if (tapeToWrite == firstTape) { // update latest area for the tape
                            lastAreaTape1 = prevArea;
                        } else {
                            lastAreaTape2 = prevArea;
                        }
                        // Switch to the other tape
                        tapeToWrite = (tapeToWrite == firstTape) ? secondTape : firstTape;

                        // check if there will be no joined runs (if yes, decrement run count)
                        Record r = ram.readRecordFromBlock(index, blockOfInput);
                        if (tapeToWrite == firstTape && r.getArea() > lastAreaTape1) {
                            tapeToWrite.runCount--;
                        } else if (tapeToWrite == secondTape && r.getArea() > lastAreaTape2) {
                            tapeToWrite.runCount--;
                        }

                        // Update Fibonacci sequence
                        currentFib = fib1 + fib2;
                        fib1 = fib2;
                        fib2 = currentFib;
                    }
                }


                // Write the record to the appropriate tape's buffer
                BlockOfMemory blockToWrite = (tapeToWrite == firstTape) ? blockFirstTape : blockSecondTape;
                if (blockToWrite.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
                    // Buffer is full: write it to the tape and clear it
                    ram.writeToFile(tapeToWrite, blockToWrite, 0);
                    blockToWrite.clear();
                }

                ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record);
                prevArea = totalArea; // Update the previous area
            }

            // Load the next block of input data if the current block is fully processed
            if (index >= blockOfInput.getSize()) {
                blockOfInput = ram.loadToBuffer(inputTape);
                if (blockOfInput != null) {
                    ram.setBlockInput(blockOfInput);
                    index = 0; // Reset the index for the new block
                }
            }
        }

        // Write any remaining data in buffers to the tapes
        if (blockFirstTape.getSize() > 0) {
            ram.writeToFile(firstTape, blockFirstTape, 0);
        }
        if (blockSecondTape.getSize() > 0) {
            ram.writeToFile(secondTape, blockSecondTape, 0);
        }

        // Ensure Fibonacci sequence consistency
        while (tapeToWrite.getRunCount() < fib2) {
            tapeToWrite.runCount++;
        }
    }


    public void mergeTapes(DiskFile firstTape, DiskFile secondTape, DiskFile _tapeToWriteTo) throws IOException {
        // Buffers for reading from tapes and writing to the output tape
        BlockOfMemory blockTape1 = ram.loadToBuffer(firstTape);
        BlockOfMemory blockTape2 = ram.loadToBuffer(secondTape);
        BlockOfMemory blockToWrite = new BlockOfMemory();
        _tapeToWriteTo.resetFileOutputStream();

        int indexTape1 = 0, indexTape2 = 0;
        int prevAreaTape1 = -1, prevAreaTape2 = -1;
        int numOfRuns = Math.min(firstTape.runCount, secondTape.runCount);
        // Continue merging until both tapes are exhausted
        for (int i = 0; i < numOfRuns; i++) {
            Record record1 = null, record2 = null;
            record1 = ram.readRecordFromBlock(indexTape1, blockTape1);
            indexTape1 += Record.RECORD_SIZE;
            record2 = ram.readRecordFromBlock(indexTape2, blockTape2);
            indexTape2 += Record.RECORD_SIZE;

            while (true) {
                if (record1.getArea() < record2.getArea()) {
                    ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record1);
                    prevAreaTape1 = record1.getArea();
                    record1 = ram.readRecordFromBlock(indexTape1, blockTape1);
                    if (record1.getArea() < prevAreaTape1) {
                        firstTape.runCount--;
                        while (record2.getArea() >= prevAreaTape2) {
                            ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record2);
                            prevAreaTape2 = record2.getArea();
                            record2 = ram.readRecordFromBlock(indexTape2, blockTape2);
                            if (record2.getArea() < prevAreaTape2) {
                                secondTape.runCount--;
                                break;
                            }
                            else {
                                indexTape2 += Record.RECORD_SIZE;
                            }
                        }
                        break;
                    }
                    else {
                        indexTape1 += Record.RECORD_SIZE;
                    }
                }
                else {
                    ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record2);
                    prevAreaTape2 = record2.getArea();
                    record2 = ram.readRecordFromBlock(indexTape2, blockTape2);
                    if (record2.getArea() < prevAreaTape2) {
                        secondTape.runCount--;
                        while (record1.getArea() >= prevAreaTape1) {
                            ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record1);
                            prevAreaTape1 = record1.getArea();
                            record1 = ram.readRecordFromBlock(indexTape1, blockTape1);
                            if (record1.getArea() < prevAreaTape1) {
                                firstTape.runCount--;
                                break;
                            }
                            else {
                                indexTape2 += Record.RECORD_SIZE;
                            }
                        }
                        break;
                    }
                    else {
                        indexTape2 += Record.RECORD_SIZE;
                    }
                }
            }

            // Write the output buffer to the tape if full
            if (blockToWrite.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
                ram.writeToFile(_tapeToWriteTo, blockToWrite, 0);
                blockToWrite.clear();
            }

            // Reload tape 1's buffer if necessary
            if (blockTape1 != null && indexTape1 >= blockTape1.getSize()) {
                blockTape1 = ram.loadToBuffer(firstTape);
                indexTape1 = 0;
            }

            // Reload tape 2's buffer if necessary
            if (blockTape2 != null && indexTape2 >= blockTape2.getSize()) {
                blockTape2 = ram.loadToBuffer(secondTape);
                indexTape2 = 0;
            }
        }

        // Write any remaining data in the output buffer to the tape
        if (blockToWrite.getSize() > 0) {
            ram.writeToFile(_tapeToWriteTo, blockToWrite, 0);
        }

        // Write the remaining data from the non-empty tape to the output tape and clear the other tape

        while (blockTape1 != null) {
            ram.writeToFile(_tapeToWriteTo, blockTape1, indexTape1);
            blockTape1 = ram.loadToBuffer(firstTape);
            indexTape1 = 0;
        }

        while (blockTape2 != null) {
            ram.writeToFile(_tapeToWriteTo, blockTape2, indexTape2);
            blockTape2 = ram.loadToBuffer(secondTape);
            indexTape2 = 0;
        }

    }


    public void sort() throws IOException {
        divideIntoTapes(tape1, tape2);

        mergeTapes(tape1, tape2, inputTape);
        phaseCount++;
//        while (tape1.runCount + tape2.runCount != 1) {
//            mergeTapes(tape1, tape2, inputTape);
//            phaseCount++;
//            // Swap the tapes for the next phase
//            DiskFile temp = tape1;
//            tape1 = inputTape;
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
