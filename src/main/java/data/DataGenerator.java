package data;

import java.io.IOException;

public interface DataGenerator {
    void generateData(String filename, int n) throws IOException;
}
