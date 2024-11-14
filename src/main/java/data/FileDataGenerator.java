package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileDataGenerator implements DataGenerator {
    private final String sourceFile;

    public FileDataGenerator(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public void generateData(String filename, int n) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < n) {
                writer.write(line);
                writer.newLine();
                count++;
            }
        }
    }
}
