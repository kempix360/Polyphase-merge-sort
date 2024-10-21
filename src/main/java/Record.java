public class Record {
    private int length;
    private int width;
    private int height;

    public Record(int length, int width, int height) {
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public int getLength() {
        return length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getArea() {
        return 2 * (length * width + width * height + length * height);
    }

    @Override
    public String toString() {
        return length + " " + width + " " + height;
    }
}
