import data.DataGenerator;
import data.FileDataGenerator;
import data.KeyboardDataGenerator;
import data.RandomDataGenerator;
import memory.RAM;

import java.io.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        MainHelper mainHelper = new MainHelper();
        RAM ram = new RAM();
        String inputFile = "input.txt";
        String tape1File = "tapes\\tape1.txt";
        String tape2File = "tapes\\tape2.txt";
        String tape3File = "tapes\\tape3.txt";
        PolyphaseSort polyphaseSort = new PolyphaseSort(inputFile, tape1File, tape2File, tape3File, ram);
        new FileWriter(inputFile).close();
        new FileWriter(tape1File).close();
        new FileWriter(tape2File).close();
        new FileWriter(tape3File).close();


        mainHelper.generateDataToFile(inputFile);
        polyphaseSort.sort();

//        ram.loadToBuffer(inputFile, ram.getBlockInput());
//        ram.writeToFile(inputFile, ram.getBlockInput());

        System.out.println("Data before sort:");
        printFile(inputFile);
    }

    private static void printFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}