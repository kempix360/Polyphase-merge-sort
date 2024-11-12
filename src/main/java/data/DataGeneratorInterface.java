package data;

import java.io.IOException;

public interface DataGeneratorInterface {
    void generateData(String filename, int n) throws IOException;
}
