import java.io.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        int option;
        RAM ram = new RAM();
        System.out.println("Choose option:");
        System.out.println("1. Generate random data");
        System.out.println("2. Insert data from keyboard");
        System.out.println("3. Load data from a text file");
        while (true) {
            try {
                option = scanner.nextInt(); // Try reading the input
                if (option < 1 || option > 3) {  // Ensure the option is valid
                    System.out.println("Invalid option. Please choose between 1 and 3.");
                } else {
                    break;  // Exit loop if valid option
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();  // Clear the invalid input from scanner buffer
            }
        }

        String inputFile = "input.txt";
        String tape1File = "src\\main\\java\\tapes\\tape1.txt";
        String tape2File = "src\\main\\java\\tapes\\tape2.txt";
        String tape3File = "src\\main\\java\\tapes\\tape3.txt";
        new FileWriter(inputFile).close();
        new FileWriter(tape1File).close();
        new FileWriter(tape2File).close();
        new FileWriter(tape3File).close();

        if (option == 1) {
            int n;
            while (true) {
                try {
                    System.out.print("How much data do you want to generate? ");
                    n = scanner.nextInt();
                    if (n <= 0) {
                        System.out.println("Please enter a positive number.");
                    } else {
                        break;  // Valid number, break out of the loop
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    scanner.next();  // Clear invalid input
                }
            }
            DataGenerator.generateRandomData(inputFile, n);

        } else if (option == 2) {
            int n;
            while (true) {
                try {
                    System.out.print("How much data do you want to insert? ");
                    n = scanner.nextInt();
                    if (n <= 0) {
                        System.out.println("Please enter a positive number.");
                    } else {
                        break;  // Valid number, break out of the loop
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    scanner.next();  // Clear invalid input
                }
            }
            DataGenerator.generateDataFromKeyboard(inputFile, n);

        } else {
            String testFile;
            System.out.print("Provide the name of the file: ");
            while (true) {
                testFile = scanner.next();
                if (testFile.trim().isEmpty()) {
                    System.out.println("File name cannot be empty. Please provide a valid name.");
                } else {
                    break;  // Valid input, break out of the loop
                }
            }
            DataGenerator.generateDataFromFile(inputFile, testFile);
        }

        ram.loadFromFile(inputFile);

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