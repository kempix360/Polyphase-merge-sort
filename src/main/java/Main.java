import data.DataGenerator;
import data.FileDataGenerator;
import data.KeyboardDataGenerator;
import data.RandomDataGenerator;
import memory.RAM;
import memory.Record;

import java.io.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        MainHelper mainHelper = new MainHelper();
        RAM ram = new RAM();
        String inputFile = "disk_files\\input.txt";
        String tape1File = "disk_files\\tape1.txt";
        String tape2File = "disk_files\\tape2.txt";
        PolyphaseSort polyphaseSort = new PolyphaseSort(inputFile, tape1File, tape2File, ram);
        new FileWriter(inputFile).close();
        new FileWriter(tape1File).close();
        new FileWriter(tape2File).close();


        mainHelper.generateDataToFile(inputFile);

        polyphaseSort.sort();

//        System.out.println("\nData after sort:");
//        printFile(inputFile);
//
//        System.out.println("\nTape1:");
//        printFile(tape1File);
//
//        System.out.println("\nTape2:");
//        printFile(tape2File);


    }

    public static void printFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int prevArea = -1;
            System.out.println("New run");
            int runCount = 1;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+"); // Split by whitespace
                int length = Integer.parseInt(values[0]);
                int width = Integer.parseInt(values[1]);
                int height = Integer.parseInt(values[2]);
                Record record = new Record(length, width, height);
                if (record.getArea() < prevArea) {
                    System.out.println("New run");
                    runCount++;
                }
                prevArea = record.getArea();
                System.out.println(record);
            }

            System.out.println("Total number of runs: " + runCount);
        }
    }
}