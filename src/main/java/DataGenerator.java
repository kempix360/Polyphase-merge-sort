import java.io.*;
import java.util.Random;
import java.util.Scanner;

class DataGenerator {
    public static void generateRandomData(String filename, int n) throws IOException {
        Random rand = new Random();
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < n; i++) {
                int number = rand.nextInt(100) + 1; // generates numbers from range 1-100.
                writer.write(Integer.toString(number));
                number = rand.nextInt(100) + 1;
                writer.write(" " + Integer.toString(number));
                number = rand.nextInt(100) + 1;
                writer.write(" " + Integer.toString(number));
                writer.newLine();
            }
        }
    }

    public static void generateDataFromKeyboard(String filename, int n) throws IOException {
        Scanner scanner = new Scanner(System.in);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.print("Enter 3 dimensions of a rectangular prism in separate lines:");
            for (int i = 0; i < n; i++) {
                int number = scanner.nextInt();
                writer.write(Integer.toString(number));
                number = scanner.nextInt();
                writer.write(" " + Integer.toString(number));
                number = scanner.nextInt();
                writer.write(" " + Integer.toString(number));
                writer.newLine();
            }
        }
    }

    public static void generateDataFromFile(String filename, String testFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(testFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
