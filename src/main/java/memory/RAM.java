package memory;

import java.io.*;
import java.util.Scanner;

public class RAM {
    private final String filename = "input.txt";
    private BlockOfMemory blockInput;
    private BlockOfMemory blockTape1;
    private BlockOfMemory blockTape2;
    private BlockOfMemory blockTape3;
    private Scanner scanner;

    public RAM() throws FileNotFoundException {
        scanner = new Scanner(new FileInputStream(filename));
        blockInput = new BlockOfMemory();
        blockTape1 = new BlockOfMemory();
        blockTape2 = new BlockOfMemory();
        blockTape3 = new BlockOfMemory();
    }

    public BlockOfMemory loadToBuffer(DiskFile file) {
        // Initialize fileInputStream and scanner if not already done
        byte[] buffer = new byte[BlockOfMemory.BUFFER_SIZE];
        int bufferIndex = 0;

        // Loop through the file reading each integer as a token
        while (scanner.hasNextInt() && bufferIndex < BlockOfMemory.BUFFER_SIZE) {
            int number = scanner.nextInt();

            // Convert the integer to 4 bytes
            buffer[bufferIndex] = (byte) (number >> 24);
            buffer[bufferIndex + 1] = (byte) (number >> 16);
            buffer[bufferIndex + 2] = (byte) (number >> 8);
            buffer[bufferIndex + 3] = (byte) number;
            bufferIndex += 4;
        }

        // If we’ve read data, return a new BlockOfMemory
        if (bufferIndex > 0) {
            return new BlockOfMemory(buffer, bufferIndex);
        } else {
            return null; // No more data to read
        }
    }


    public void writeToFile(DiskFile file, BlockOfMemory _blockOfMemory) {
        try {
            DataOutputStream outputStream = new DataOutputStream(file.getFileOutputStream());
            byte[] data = _blockOfMemory.getBuffer();
            int size = _blockOfMemory.getSize();

            for (int i = 0; i < size; i += 4) {
                if (i + 3 < size) {
                    int number = ((data[i] & 0xFF) << 24) |
                            ((data[i + 1] & 0xFF) << 16) |
                            ((data[i + 2] & 0xFF) << 8) |
                            (data[i + 3] & 0xFF);
                    outputStream.writeBytes(number + " ");

                    // After every third integer, write a newline
                    if ((i / 4 + 1) % 3 == 0) {
                        outputStream.writeBytes("\n");
                    }

                    data[i] = 0;
                    data[i + 1] = 0;
                    data[i + 2] = 0;
                    data[i + 3] = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Record readRecordFromBlock(int index, BlockOfMemory blockOfMemory) {
        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int recordSize = Record.RECORD_SIZE;

        int[] record_values = new int[3];
        int values_index = 0;

        if (index < 0 || (index + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return null;
        }

        for (int i = index; i < index + recordSize; i += 4) {

            if (i + 3 < size) {
                int number = ((data[i] & 0xFF) << 24) |
                        ((data[i + 1] & 0xFF) << 16) |
                        ((data[i + 2] & 0xFF) << 8) |
                        (data[i + 3] & 0xFF);
                record_values[values_index] = number;
                values_index++;
            }
        }

        return new Record(record_values[0], record_values[1], record_values[2]);
    }

    public void writeRecordToBlock(int index, BlockOfMemory blockOfMemory, Record record) {
        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int recordSize = Record.RECORD_SIZE;

        if (index < 0 || (index + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return;
        }

        int[] record_values = { record.getFirst(), record.getSecond(), record.getThird() };
        int values_index = 0;

        for (int i = index; i < index + recordSize; i += 4) {
            int number = record_values[values_index];
            data[i] = (byte) (number >> 24);
            data[i + 1] = (byte) (number >> 16);
            data[i + 2] = (byte) (number >> 8);
            data[i + 3] = (byte) number;
            values_index++;
        }

        blockOfMemory.setSize(size + recordSize);

    }

    public BlockOfMemory getBlockInput() {
        return blockInput;
    }

    public void setBlockInput(BlockOfMemory blockInput) {
        this.blockInput = blockInput;
    }

    public BlockOfMemory getBlockTape1() {
        return blockTape1;
    }

    public void setBlockTape1(BlockOfMemory blockTape1) {
        this.blockTape1 = blockTape1;
    }

    public BlockOfMemory getBlockTape2() {
        return blockTape2;
    }

    public void setBlockTape2(BlockOfMemory blockTape2) {
        this.blockTape2 = blockTape2;
    }

    public BlockOfMemory getBlockTape3() {
        return blockTape3;
    }

    public void setBlockTape3(BlockOfMemory blockTape3) {
        this.blockTape3 = blockTape3;
    }
}
