public class BlockOfMemory {
    private byte[] data;
    private int size; // Actual size of data in the block

    public BlockOfMemory(byte[] data, int size) {
        this.data = new byte[size];
        System.arraycopy(data, 0, this.data, 0, size);
        this.size = size;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return size;
    }
}
