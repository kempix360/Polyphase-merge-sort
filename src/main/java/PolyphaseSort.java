import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PolyphaseSort {
    private String fileToSort = "input.txt";
    private Tape tape1;
    private Tape tape2;
    private Tape tape3;
    private RAM ram = new RAM();
    private int phaseCount = 0;
    private int totalReadOperations = 0;
    private int totalWriteOperations = 0;

    public PolyphaseSort(String _fileToSort, String tape1File, String tape2File, String tape3File, RAM _ram) throws IOException {
        fileToSort = _fileToSort;
        tape1 = new Tape(tape1File);
        tape2 = new Tape(tape2File);
        tape3 = new Tape(tape3File);
        ram = _ram;
    }

    public void divideIntoTapes(Tape _writeTape1, Tape _writeTape2) throws IOException {
        // _readTape: the tape we are reading from
        // _writeTape1 and _writeTape1: the tapes we are writing to
        ram.loadFromFile(fileToSort);
        Record record = ram.getRecordsInMemory().remove(0);
        totalReadOperations++;

        int fib1 = 1, fib2 = 1;
        int currentFib = fib1;
        int runCount = 0;
        int prevArea = -1;
        Tape tapeToAdd = _writeTape1;

        while (record != null) {
            int totalArea = record.getArea();
            // run.addRecord(record);
            // write to a proper buffer
            totalWriteOperations++;

            if (totalArea < prevArea) {
                tapeToAdd.runCount++;
            }

            if (tapeToAdd.runCount == currentFib) {
                tapeToAdd = (tapeToAdd == _writeTape1) ? _writeTape2 : _writeTape1;

                // Update Fibonacci sequence for the next run
                currentFib = fib1 + fib2;
                fib1 = fib2;
                fib2 = currentFib;

            }

            prevArea = totalArea;
            record = ram.getRecordsInMemory().remove(0);
            totalReadOperations++;
        }

        while (tapeToAdd.getRunCount() < fib2) {
            tapeToAdd.runCount++;
        }

    }

    public void mergeTapes(Tape _tape1, Tape _tape2, Tape _tapeToWriteTo) throws IOException {

    }


    public void sort() throws IOException {
        divideIntoTapes(initialTape, tape1, tape2);
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
        tape3.close();
        tape1.close();
        tape2.close();
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