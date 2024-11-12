import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Tape {
    private final String filename;
    private BufferedReader reader;
    private BufferedWriter writer;
    int runCount = 0;
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

    public void close() throws IOException {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
    }

    public String getFilename() {
        return filename;
    }

    public int getRunCount() {
        return runCount;
    }

    public int getReadOperations() {
        return readOperations;
    }

    public int getWriteOperations() {
        return writeOperations;
    }

}
