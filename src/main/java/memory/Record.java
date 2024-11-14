package memory;

public class Record {
    public static final int RECORD_SIZE = 12;
    private final int first;
    private final int second;
    private final int third;

    public Record(int length, int width, int height) {
        this.first = length;
        this.second = width;
        this.third = height;
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    public int getThird() {
        return third;
    }

    public int getArea() {
        return 2 * (first * second + second * third + first * third);
    }

    @Override
    public String toString() {
        return first + " " + second + " " + third + " Area: " + getArea();
    }

    public boolean compareTo(Record record2) {
        return this.getArea() < record2.getArea();
    }
}
