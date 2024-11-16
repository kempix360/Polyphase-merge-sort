package memory;

import java.io.*;
import java.util.Scanner;

public class DiskFile {
    private final String filename;
    private FileInputStream fileInputStream = null;
    private FileOutputStream fileOutputStream = null;
    private Scanner scanner = null;
    public int runCount = 0;
    private int readOperations = 0;
    private int writeOperations = 0;

    public DiskFile(String filename) throws IOException {
        this.filename = filename;
        fileInputStream = new FileInputStream(filename);
        fileOutputStream = new FileOutputStream(filename);
        scanner = new Scanner(fileInputStream);
        reset();
    }

    public void reset() throws IOException {
        close();
        fileInputStream = new FileInputStream(filename);
    }

    public void close() throws IOException {
        if (fileInputStream != null) fileInputStream.close();
    }

    public String getFilename() {
        return filename;
    }

    public FileInputStream getFileInputStream() {
        return fileInputStream;
    }

    public void setFileInputStream(FileInputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public FileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void setFileOutputStream(FileOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
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
