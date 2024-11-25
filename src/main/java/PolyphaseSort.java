import memory.BlockOfMemory;
import memory.RAM;
import memory.Record;
import memory.DiskFile;

import java.io.*;
import java.util.Objects;
import java.nio.file.*;
import java.util.stream.Stream;

class PolyphaseSort {
    private final String fileToSortPath;
    private DiskFile tape1;
    private DiskFile tape2;
    private DiskFile tape3;
    private BlockOfMemory blockTape1;
    private BlockOfMemory blockTape2;
    private BlockOfMemory blockTape3;
    private final RAM ram;
    private int phaseCount = 0;
    private int initialNumOfRuns;

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

        System.out.println("\n---------------------------------");
        System.out.println(YELLOW + "Data before sort:" + RESET);
        initialNumOfRuns = printFile(fileToSortPath);
        System.out.println("\n---------------------------------");
        System.out.println(CYAN + "Sorting in progress..." + RESET);

        divideIntoTapes();
        System.out.println("Initial distribution completed. Read operations: "
                + ram.getTotalReadOperations() + ", Write operations: " + ram.getTotalWriteOperations());

        while (tape1.getRunCount() + tape2.getRunCount() + tape3.getRunCount() != 1) {
            mergeTapes();
            // Swap the tapes for the next phase
            DiskFile temp;
            if (tape1.getRunCount() > tape2.getRunCount()) {
                temp = tape2;
                tape2 = tape3;
            }
            else {
                temp = tape1;
                tape1 = tape3;
            }
            tape3 = temp;
            phaseCount++;

            System.out.println("Phase " + phaseCount + " completed. Read operations: "
                    + ram.getTotalReadOperations() + ", Write operations: " + ram.getTotalWriteOperations());
            System.out.println("Number of runs on tape 1: " + tape1.getRunCount());
            System.out.println("Number of runs on tape 2: " + tape2.getRunCount());

//            while (true) {
//                System.out.println("Do you want to print the content of the tapes? (y/n)");
//                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//                String answer = reader.readLine();
//                if (answer.equals("y")) {
//                    System.out.println("\n---------------------------------");
//                    System.out.println(YELLOW + "Data after phase " + phaseCount + ":" + RESET);
//                    printFile(tape1.getFilename());
//                    printFile(tape2.getFilename());
//                    printFile(tape3.getFilename());
//                    System.out.println("\n---------------------------------");
//                    break;
//                } else if (answer.equals("n")) {
//                    break;
//                } else {
//                    System.out.println("Invalid input. Please enter 'y' or 'n'.");
//                }
//            }
        }

        DiskFile finalTape = (tape1.getRunCount() == 1) ? tape1 : tape2;
        if (!Objects.equals(finalTape.getFilename(), fileToSortPath))
            writeContentToAnotherFile(fileToSortPath, finalTape.getFilename());

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
        int lastAreaTape1 = Integer.MAX_VALUE, lastAreaTape2 = Integer.MAX_VALUE;
        DiskFile tapeToWrite = tape1;

        while (blockTape3 != null) {
            // Process the current block until it's exhausted
            while (blockTape3.getIndex() < blockTape3.getSize()) {
                // Read the next record from the current block
                Record record = ram.readRecordFromBlock(blockTape3);
                blockTape3.setIndex(blockTape3.getIndex() + Record.RECORD_SIZE);
                int totalArea = record.getArea();

                // Check for a new run
                if (totalArea < prevArea) {
                    tapeToWrite.incrementRunCount();
                    if (tapeToWrite.getRunCount() == currentFib) {
                        if (tapeToWrite == tape1) { // update latest area for the tape
                            lastAreaTape1 = prevArea;
                        } else {
                            lastAreaTape2 = prevArea;
                        }
                        // Switch to the other tape
                        tapeToWrite = (tapeToWrite == tape1) ? tape2 : tape1;

                        // check if there will be no joined runs (if yes, decrement run count)
                        if (tapeToWrite == tape1 && record.getArea() > lastAreaTape1) {
                            tapeToWrite.decrementRunCount();
                        } else if (tapeToWrite == tape2 && record.getArea() > lastAreaTape2) {
                            tapeToWrite.decrementRunCount();
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
                    ram.writeToFile(tapeToWrite, blockToWrite);
                    blockToWrite.clear();
                }

                ram.writeRecordToBlock(blockToWrite, record);
                prevArea = totalArea; // Update the previous area
            }

            // Load the next block of input data if the current block is fully processed
            if (blockTape3.getIndex() >= blockTape3.getSize()) {
                blockTape3 = ram.loadToBuffer(tape3);
                if (blockTape3 != null) blockTape3.setIndex(0);
            }
        }

        // Write any remaining data in buffers to the tapes
        if (blockTape1.getSize() > 0) {
            ram.writeToFile(tape1, blockTape1);
        }
        if (blockTape2.getSize() > 0) {
            ram.writeToFile(tape2, blockTape2);
        }

        // Ensure Fibonacci sequence consistency
        while (tapeToWrite.getRunCount() < fib1) {
            tapeToWrite.incrementRunCount();
        }
    }


    public void mergeTapes() throws IOException {
        // Buffers for reading from tapes and writing to the output tape
        tape1.resetFileInputStream();
        tape2.resetFileInputStream();
        tape1.resetScanner();
        tape2.resetScanner();
        tape3.resetFileOutputStream();

        int numOfReadsTape1 = 0, numOfReadsTape2 = 0;
        int prevAreaTape1, prevAreaTape2;
        int numOfRuns = Math.min(tape1.getRunCount(), tape2.getRunCount());

        blockTape1 = ram.loadToBuffer(tape1);
        numOfReadsTape1++;
        blockTape2 = ram.loadToBuffer(tape2);
        numOfReadsTape2++;
        blockTape3 = new BlockOfMemory();


        Record record1, record2;
        // Fetch the first records from the tapes
        record1 = (blockTape1 != null) ? ram.readRecordFromBlock(blockTape1) : null;
        if (blockTape1 != null) blockTape1.setIndex(blockTape1.getIndex() + Record.RECORD_SIZE);
        record2 = (blockTape2 != null) ? ram.readRecordFromBlock(blockTape2) : null;
        if (blockTape2 != null) blockTape2.setIndex(blockTape2.getIndex() + Record.RECORD_SIZE);


        boolean isEndRun1, isEndRun2;
        // Continue merging until reached number of runs
        for (int i = 0; i < numOfRuns; i++) {
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
                    writeNextRecord(tape3, record1);
                    prevAreaTape1 = record1.getArea();

                    // read next record from tape1 and reload block if needed
                    if (blockTape1 != null && blockTape1.getIndex() >= blockTape1.getSize()) {
                        blockTape1 = ram.loadToBuffer(tape1);
                        if (blockTape1 != null) {
                            blockTape1.setIndex(0);
                            numOfReadsTape1++;
                        }
                    }
                    record1 = (blockTape1 != null) ? ram.readRecordFromBlock(blockTape1) : null;
                    if (blockTape1 != null) blockTape1.setIndex(blockTape1.getIndex() + Record.RECORD_SIZE);

                    if (record1 == null || record1.getArea() < prevAreaTape1) {
                        isEndRun1 = true;
                    }
                } else {
                    writeNextRecord(tape3, record2);
                    prevAreaTape2 = record2.getArea();

                    // read next record from tape2 and reload block if needed
                    if (blockTape2 != null && blockTape2.getIndex() >= blockTape2.getSize()) {
                        blockTape2 = ram.loadToBuffer(tape2);
                        if (blockTape2 != null) {
                            blockTape2.setIndex(0);
                            numOfReadsTape2++;
                        }
                    }
                    record2 = (blockTape2 != null) ? ram.readRecordFromBlock(blockTape2) : null;
                    if (blockTape2 != null) blockTape2.setIndex(blockTape2.getIndex() + Record.RECORD_SIZE);

                    if (record2 == null || record2.getArea() < prevAreaTape2) {
                        isEndRun2 = true;
                    }
                }
            }
            tape1.decrementRunCount();
            tape2.decrementRunCount();
            tape3.incrementRunCount();
        }

        // Write any remaining data in the output buffer to the tape
        if (blockTape3.getSize() > 0) {
            ram.writeToFile(tape3, blockTape3);
        }

        // overwrite the tape with the remaining records
        int recordsPerBlock = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int totalRecordsProcessedTape1 = numOfReadsTape1 * recordsPerBlock;
        int totalRecordsProcessedTape2 = numOfReadsTape2 * recordsPerBlock;
        String tempFileName = "temp.in";
        DiskFile tempDiskFile = new DiskFile(tempFileName);
        DiskFile tapeToOverwrite = (tape1.getRunCount() > 0) ? tape1 : tape2;

        if (blockTape1 != null && tape1.getRunCount() > 0) {
            if (blockTape1.getIndex() != 0) blockTape1.setIndex(blockTape1.getIndex() - Record.RECORD_SIZE);
            ram.writeToFile(tempDiskFile, blockTape1);
            copyRemainingRecords(tapeToOverwrite, tempDiskFile, totalRecordsProcessedTape1);
        }
        else tape1.resetFileOutputStream();
        if (blockTape2 != null && tape2.getRunCount() > 0) {
            if (blockTape2.getIndex() != 0) blockTape2.setIndex(blockTape2.getIndex() - Record.RECORD_SIZE);
            ram.writeToFile(tempDiskFile, blockTape2);
            copyRemainingRecords(tapeToOverwrite, tempDiskFile, totalRecordsProcessedTape2);
        }
        else tape2.resetFileOutputStream();

        writeContentToAnotherFile(tapeToOverwrite.getFilename(), tempFileName);
    }


    private void writeNextRecord (DiskFile tape, Record record) {
        if (blockTape3.getSize() + Record.RECORD_SIZE > BlockOfMemory.BUFFER_SIZE) {
            ram.writeToFile(tape, blockTape3);
            blockTape3.clear();
        }
        ram.writeRecordToBlock(blockTape3, record);
    }

    public void printStats() {
        System.out.println("Initial number of runs: " + initialNumOfRuns);
        System.out.println("Number of sort phases: " + phaseCount);
        System.out.println("Number of read operations: " + ram.getTotalReadOperations());
        System.out.println("Number of write operations: " + ram.getTotalWriteOperations());
        System.out.println("Total number of I/O operations: " + (ram.getTotalReadOperations() + ram.getTotalWriteOperations()));
    }

    public static int printFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int prevArea = -1;
//            System.out.println("New run");
            int runCount = 1;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+"); // Split by whitespace
                int length = Integer.parseInt(values[0]);
                int width = Integer.parseInt(values[1]);
                int height = Integer.parseInt(values[2]);
                Record record = new Record(length, width, height);
                if (record.getArea() < prevArea) {
//                    System.out.println("New run");
                    runCount++;
                }
                prevArea = record.getArea();
                System.out.println(record);
            }
            return runCount;
        }
    }

    private void copyRemainingRecords(DiskFile sourceTape, DiskFile targetTape, int startLine) throws IOException {
        Path sourcePath = Paths.get(sourceTape.getFilename());
        Path targetPath = Paths.get(targetTape.getFilename());

        try (
                Stream<String> lines = Files.lines(sourcePath);
                BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        ) {
            lines.skip(startLine).forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }


    public void writeContentToAnotherFile(String file1, String file2) throws IOException {
        File fileToOverwrite = new File(file1);
        File fileToCopy = new File(file2);
        try (FileInputStream fis = new FileInputStream(fileToCopy);
             FileOutputStream fos = new FileOutputStream(fileToOverwrite)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

}
