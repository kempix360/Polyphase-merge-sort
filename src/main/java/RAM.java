import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RAM {
    private List<BlockOfMemory> blocksOfMemory;
    private List<Record> recordsInMemory;
    private final int BUFFER_SIZE;

    public RAM() {
        this.BUFFER_SIZE = 1024;
        this.blocksOfMemory = new ArrayList<>();
    }

    // Method to read a block of data from the file into memory
    public boolean loadFromFile(String filePath) {
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                BlockOfMemory block = new BlockOfMemory(buffer, bytesRead);
                blocksOfMemory.add(block);
                parseBlockToRecords(block);
            }
            return true; // Successfully read file
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Error reading file
        }
    }

    private void parseBlockToRecords(BlockOfMemory block) {
        String dataString = new String(block.getData(), 0, block.getSize()); // Convert block to String
        String[] lines = dataString.split("\n"); // Split into lines (records)

        for (String line : lines) {
            String[] parts = line.trim().split(" ");
            if (parts.length == 3) { // Assuming each record has 3 numbers
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                int third = Integer.parseInt(parts[2]);
                Record record = new Record(first, second, third);
                recordsInMemory.add(record);
            }
        }
    }

    // Method to write all blocks from memory back to the file
    public boolean writeToFile(String filePath) {
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            for (BlockOfMemory block : blocksOfMemory) {
                outputStream.write(block.getData(), 0, block.getSize());
            }
            return true; // Successfully wrote to file
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Error writing to file
        }
    }

    public List<Record> getRecordsInMemory() {
        return recordsInMemory;
    }

    // Access the list of blocks in memory
    public List<BlockOfMemory> getBlocksOfMemory() {
        return blocksOfMemory;
    }
}
