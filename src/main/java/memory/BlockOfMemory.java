package memory;

public class BlockOfMemory {
    public static final int BUFFER_SIZE = 2400;
    private final byte[] buffer;
    private final int size; // Actual size of data in the block

    public BlockOfMemory() {
        this.buffer = new byte[BUFFER_SIZE];
        this.size = 0;
    }

    public BlockOfMemory(byte[] data, int size) {
        this.buffer = new byte[size];
        System.arraycopy(data, 0, this.buffer, 0, size);
        this.size = size;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        if (size == 0) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            if (buffer[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isFull() {
        return size == BUFFER_SIZE;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            buffer[i] = 0;
        }
    }

}
