import memory.BlockOfMemory;
import memory.RAM;
import memory.Record;
import memory.DiskFile;

import java.io.IOException;

class PolyphaseSort {
    private DiskFile fileToSort;
    private DiskFile tape1;
    private DiskFile tape2;
    private DiskFile tape3;
    private RAM ram;
    private int phaseCount = 0;
    private int totalReadOperations = 0;
    private int totalWriteOperations = 0;

    public PolyphaseSort(String _fileToSort, String tape1File, String tape2File, String tape3File, RAM _ram) throws IOException {
        fileToSort = new DiskFile(_fileToSort);
        tape1 = new DiskFile(tape1File);
        tape2 = new DiskFile(tape2File);
        tape3 = new DiskFile(tape3File);
        ram = _ram;
    }

    public void divideIntoTapes(DiskFile firstTape, DiskFile secondTape) throws IOException {
        // Initialize buffers for the tapes
        BlockOfMemory blockFirstTape = new BlockOfMemory();
        BlockOfMemory blockSecondTape = new BlockOfMemory();

        // Load the first block of input data
        BlockOfMemory blockOfInput = ram.loadToBuffer(fileToSort);
        if (blockOfInput == null) {
            throw new IOException("File is empty or cannot be loaded.");
        }

        int fib1 = 1, fib2 = 1; // Fibonacci sequence initialization
        int currentFib = fib1;
        int prevArea = -1; // Initialize the previous record's area for run comparison
        int index = 0; // Index within the current block
        int counterOfRecords = 0;
        DiskFile tapeToWrite = firstTape;

        while (blockOfInput != null) {
            // Process the current block until it's exhausted
            while (index < blockOfInput.getSize()) {
                // Read the next record from the current block
                Record record = ram.readRecordFromBlock(index, blockOfInput);
                counterOfRecords++;
                index += Record.RECORD_SIZE;
                int totalArea = record.getArea();

                // Check for a new run
                if (totalArea < prevArea) {
                    tapeToWrite.runCount++;
                    if (tapeToWrite.runCount == currentFib) {
                        // Switch to the other tape
                        tapeToWrite = (tapeToWrite == firstTape) ? secondTape : firstTape;

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
                    ram.writeToFile(tapeToWrite, blockToWrite);
                    blockToWrite.clear();
                }

                ram.writeRecordToBlock(blockToWrite.getSize(), blockToWrite, record);
                prevArea = totalArea; // Update the previous area
            }

            // Load the next block of input data if the current block is fully processed
            if (index >= blockOfInput.getSize()) {
                blockOfInput = ram.loadToBuffer(fileToSort);
                if (blockOfInput != null) {
                    ram.setBlockInput(blockOfInput);
                    index = 0; // Reset the index for the new block
                }
            }
        }

        // Write any remaining data in buffers to the tapes
        if (blockFirstTape.getSize() > 0) {
            ram.writeToFile(firstTape, blockFirstTape);
        }
        if (blockSecondTape.getSize() > 0) {
            ram.writeToFile(secondTape, blockSecondTape);
        }

        // Ensure Fibonacci sequence consistency
        while (tapeToWrite.getRunCount() < fib2) {
            tapeToWrite.runCount++;
        }
    }


    public void mergeTapes(DiskFile _tape1, DiskFile _tape2, DiskFile _tapeToWriteTo) throws IOException {

    }


    public void sort() throws IOException {
        divideIntoTapes(tape1, tape2);
        mergeTapes(tape1, tape2, tape3);
        phaseCount++;
    }


    private void updateStats() {
        totalReadOperations += tape3.getReadOperations() + tape1.getReadOperations() + tape2.getReadOperations();
        totalWriteOperations += tape1.getWriteOperations() + tape2.getWriteOperations() + tape3.getWriteOperations();
    }

    public void printStats() {
        System.out.println("Number of sort phases: " + phaseCount);
        System.out.println("Number of read operations: " + totalReadOperations);
        System.out.println("Number of write operations: " + totalWriteOperations);
    }

    public void close() throws IOException {
        tape1.close();
        tape2.close();
        tape3.close();
    }


    // TODO:
    // 1. dividing input into 2 tapes
    // 2. merging the contents of the tapes
    // 3. saving to the initial file and counting number of phases
    // czy mam w buforze jakis rekord
    // odczyt: czytam z pliku, zapisuje do bufora, a nastepnie do tasmy
    // zapis: najpeirw do bufora, jak sie zapelni, to zapisujemy do pliku
    // liczba serii na tasme zdefiniowana (ale nie dlugosc serii)!!!!!!!!!
    // tasmy to tez sa pliki
    // na biezaco obliczamy sume pol powierzchni
}
