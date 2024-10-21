import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Tape {
    private String filename;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int readOperations = 0;
    private int writeOperations = 0;

    public Tape(String filename) throws IOException {
        this.filename = filename;
        reset();
    }

    public void reset() throws IOException {
        // closing previous streams
        if (reader != null) reader.close();
        if (writer != null) writer.close();
        reader = new BufferedReader(new FileReader(filename));
        writer = new BufferedWriter(new FileWriter(filename, true));
    }

    public int[] readRecord() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;
        readOperations++;
        String[] parts = line.split(" ");
        int[] currentRecord = new int[3];
        for (int i = 0; i < 3; i++) {
            currentRecord[i] = Integer.parseInt(parts[i]);
        }
        return currentRecord;
    }

    public void writeRecord(int[] record) throws IOException {
        writer.write(record[0] + " " + record[1] + " " + record[2]);
        writer.newLine();
        writeOperations++;
    }

    public void close() throws IOException {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
    }

    public int getReadOperations() {
        return readOperations;
    }

    public int getWriteOperations() {
        return writeOperations;
    }
}
